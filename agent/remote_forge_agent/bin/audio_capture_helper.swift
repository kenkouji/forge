import Foundation
import ScreenCaptureKit
import CoreMedia
import AVFoundation

class AudioCaptureHelper: NSObject, SCStreamOutput {
    let outHandle = FileHandle.standardOutput

    func stream(_ stream: SCStream, didOutputSampleBuffer sampleBuffer: CMSampleBuffer, of outputType: SCStreamOutputType) {
        guard outputType == .audio else { return }

        let numSamples = CMSampleBufferGetNumSamples(sampleBuffer)
        guard numSamples > 0 else { return }

        guard let formatDesc = CMSampleBufferGetFormatDescription(sampleBuffer),
              let asbd = CMAudioFormatDescriptionGetStreamBasicDescription(formatDesc)?.pointee else {
            return
        }

        var neededSize: Int = 0
        let statusNeeded = CMSampleBufferGetAudioBufferListWithRetainedBlockBuffer(
            sampleBuffer,
            bufferListSizeNeededOut: &neededSize,
            bufferListOut: nil,
            bufferListSize: 0,
            blockBufferAllocator: nil,
            blockBufferMemoryAllocator: nil,
            flags: 0,
            blockBufferOut: nil
        )

        guard neededSize > 0 else { return }

        let rawMemory = UnsafeMutableRawPointer.allocate(byteCount: neededSize, alignment: MemoryLayout<AudioBufferList>.alignment)
        let bufferListPtr = rawMemory.bindMemory(to: AudioBufferList.self, capacity: 1)
        defer { rawMemory.deallocate() }

        var blockBuffer: CMBlockBuffer?
        let status = CMSampleBufferGetAudioBufferListWithRetainedBlockBuffer(
            sampleBuffer,
            bufferListSizeNeededOut: nil,
            bufferListOut: bufferListPtr,
            bufferListSize: neededSize,
            blockBufferAllocator: nil,
            blockBufferMemoryAllocator: nil,
            flags: 0,
            blockBufferOut: &blockBuffer
        )

        guard status == noErr else { return }

        let ablPtr = UnsafeMutableAudioBufferListPointer(bufferListPtr)
        guard ablPtr.count >= 2,
              let leftData = ablPtr[0].mData,
              let rightData = ablPtr[1].mData else {
            return
        }

        let leftFloats = leftData.assumingMemoryBound(to: Float.self)
        let rightFloats = rightData.assumingMemoryBound(to: Float.self)

        var pcmBytes = [Int16](repeating: 0, count: numSamples * 2)
        for i in 0..<numSamples {
            let l = max(-1.0, min(1.0, leftFloats[i]))
            let r = max(-1.0, min(1.0, rightFloats[i]))
            pcmBytes[i * 2] = Int16(l * 32767.0)
            pcmBytes[i * 2 + 1] = Int16(r * 32767.0)
        }

        pcmBytes.withUnsafeBytes { rawPtr in
            if let addr = rawPtr.baseAddress {
                Darwin.write(STDOUT_FILENO, addr, rawPtr.count)
            }
        }
    }
}

let args = CommandLine.arguments
let sampleRate: Int = args.count > 1 ? (Int(args[1]) ?? 48000) : 48000
let channels: Int = args.count > 2 ? (Int(args[2]) ?? 2) : 2

// Global retain to prevent ARC deallocation
var globalStream: SCStream? = nil
let handler = AudioCaptureHelper()

Task {
    do {
        let content = try await SCShareableContent.excludingDesktopWindows(false, onScreenWindowsOnly: false)
        guard let display = content.displays.first else {
            fputs("ERROR: No display found\n", stderr)
            exit(1)
        }

        let filter = SCContentFilter(display: display, excludingApplications: [], exceptingWindows: [])
        let config = SCStreamConfiguration()
        config.capturesAudio = true
        // Set minimumFrameInterval to 60s so ScreenCaptureKit uses near-zero GPU/CPU for video
        config.minimumFrameInterval = CMTime(value: 60, timescale: 1)
        config.queueDepth = 3
        config.width = 2
        config.height = 2
        config.sampleRate = sampleRate
        config.channelCount = channels

        let stream = SCStream(filter: filter, configuration: config, delegate: nil)
        globalStream = stream
        try stream.addStreamOutput(handler, type: .audio, sampleHandlerQueue: DispatchQueue.global(qos: .userInteractive))
        try await stream.startCapture()

        fputs("READY: CAPTURING AUDIO \(sampleRate)Hz \(channels)ch\n", stderr)
        fflush(stderr)
    } catch {
        fputs("ERROR: \(error.localizedDescription)\n", stderr)
        exit(1)
    }
}

signal(SIGINT) { _ in exit(0) }
signal(SIGTERM) { _ in exit(0) }

// Keep alive until stdin closed
let stdIn = FileHandle.standardInput
let _ = stdIn.readData(ofLength: 1)
exit(0)
