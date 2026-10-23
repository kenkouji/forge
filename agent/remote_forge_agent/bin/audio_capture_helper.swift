import Foundation
import ScreenCaptureKit
import CoreMedia
import AVFoundation

class AudioCaptureHelper: NSObject, SCStreamOutput {
    let outHandle = FileHandle.standardOutput

    func stream(_ stream: SCStream, didOutputSampleBuffer sampleBuffer: CMSampleBuffer, of outputType: SCStreamOutputType) {
        guard outputType == .audio else { return }

        guard let formatDesc = CMSampleBufferGetFormatDescription(sampleBuffer),
              let asbd = CMAudioFormatDescriptionGetStreamBasicDescription(formatDesc)?.pointee else {
            return
        }

        guard let blockBuffer = CMSampleBufferGetDataBuffer(sampleBuffer) else { return }
        var length: Int = 0
        var dataPointer: UnsafeMutablePointer<Int8>?
        if CMBlockBufferGetDataPointer(blockBuffer, atOffset: 0, lengthAtOffsetOut: nil, totalLengthOut: &length, dataPointerOut: &dataPointer) == noErr,
           let ptr = dataPointer, length > 0 {

            let isFloat = (asbd.mFormatFlags & kAudioFormatFlagIsFloat) != 0

            if !isFloat && asbd.mBitsPerChannel == 16 {
                let data = Data(bytes: ptr, count: length)
                outHandle.write(data)
            } else if isFloat {
                // Convert 32-bit Float PCM to 16-bit Int PCM
                let floatCount = length / 4
                let floatPtr = ptr.withMemoryRebound(to: Float.self, capacity: floatCount) { $0 }
                var int16Data = [Int16](repeating: 0, count: floatCount)
                for i in 0..<floatCount {
                    let f = max(-1.0, min(1.0, floatPtr[i]))
                    int16Data[i] = Int16(f * 32767.0)
                }
                int16Data.withUnsafeBytes { rawBytes in
                    outHandle.write(Data(rawBytes))
                }
            }
        }
    }
}

let args = CommandLine.arguments
let sampleRate: Int = args.count > 1 ? (Int(args[1]) ?? 44100) : 44100
let channels: Int = args.count > 2 ? (Int(args[2]) ?? 2) : 2

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
        config.sampleRate = sampleRate
        config.channelCount = channels

        let stream = SCStream(filter: filter, configuration: config, delegate: nil)
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
