import Foundation
import CoreGraphics
import AppKit

// Objective-C interface declarations for private CoreGraphics CGVirtualDisplay APIs
@objc class CGVirtualDisplayMode: NSObject {
    @objc init(width: UInt32, height: UInt32, refreshRate: Double) {
        super.init()
    }
}

@objc class CGVirtualDisplaySettings: NSObject {
    @objc var modes: [Any] = []
    @objc var hiDPI: UInt32 = 0
}

@objc class CGVirtualDisplayDescriptor: NSObject {
    @objc var queue: DispatchQueue?
    @objc var name: String?
    @objc var sizeInMillimeters: CGSize = .zero
    @objc var maxPixelsWide: UInt32 = 0
    @objc var maxPixelsHigh: UInt32 = 0
    @objc var serialNum: UInt32 = 0
    @objc var productID: UInt32 = 0
    @objc var vendorID: UInt32 = 0
    @objc var terminationHandler: ((Any, Any) -> Void)?
}

@objc class CGVirtualDisplay: NSObject {
    @objc var displayID: CGDirectDisplayID { return 0 }
    @objc func apply(_ settings: CGVirtualDisplaySettings) -> Bool { return false }
    @objc init(descriptor: CGVirtualDisplayDescriptor) { super.init() }
}

let args = CommandLine.arguments
let width: UInt32 = args.count > 1 ? (UInt32(args[1]) ?? 1920) : 1920
let height: UInt32 = args.count > 2 ? (UInt32(args[2]) ?? 1080) : 1080
let fps: Double = args.count > 3 ? (Double(args[3]) ?? 60.0) : 60.0
let name: String = args.count > 4 ? args[4] : "RemoteForge Virtual Extended Display"

guard let descriptorClass = NSClassFromString("CGVirtualDisplayDescriptor") as? NSObject.Type,
      let displayClass = NSClassFromString("CGVirtualDisplay") as? NSObject.Type,
      let modeClass = NSClassFromString("CGVirtualDisplayMode") as? NSObject.Type,
      let settingsClass = NSClassFromString("CGVirtualDisplaySettings") as? NSObject.Type else {
    fputs("ERROR: CGVirtualDisplay classes unavailable\n", stderr)
    exit(1)
}

let descriptor = descriptorClass.init()
descriptor.setValue(DispatchQueue.main, forKey: "queue")
descriptor.setValue(name, forKey: "name")
descriptor.setValue(CGSize(width: 520, height: 325), forKey: "sizeInMillimeters")
descriptor.setValue(max(width, 2560), forKey: "maxPixelsWide")
descriptor.setValue(max(height, 1600), forKey: "maxPixelsHigh")
descriptor.setValue(UInt32(1001), forKey: "serialNum")
descriptor.setValue(UInt32(0x9999), forKey: "productID")
descriptor.setValue(UInt32(0x8888), forKey: "vendorID")

let displayAlloc = displayClass.perform(NSSelectorFromString("alloc")).takeUnretainedValue()
guard let display = displayAlloc.perform(NSSelectorFromString("initWithDescriptor:"), with: descriptor)?.takeUnretainedValue() else {
    fputs("ERROR: Failed to initialize CGVirtualDisplay\n", stderr)
    exit(1)
}

let displayID = display.value(forKey: "displayID") as? UInt32 ?? 0

// Construct Mode
typealias ModeInitFunc = @convention(c) (AnyObject, Selector, UInt32, UInt32, Double) -> AnyObject
let modeAlloc = modeClass.perform(NSSelectorFromString("alloc")).takeUnretainedValue()
let modeSel = NSSelectorFromString("initWithWidth:height:refreshRate:")
let modeMethod = class_getMethodImplementation(modeClass, modeSel)
let modeInit = unsafeBitCast(modeMethod, to: ModeInitFunc.self)
let modeObj = modeInit(modeAlloc, modeSel, width, height, fps)

// Configure Settings
let settings = settingsClass.init()
settings.setValue([modeObj], forKey: "modes")
settings.setValue(UInt32(0), forKey: "hiDPI")

typealias ApplyFunc = @convention(c) (AnyObject, Selector, AnyObject) -> Bool
let applySel = NSSelectorFromString("applySettings:")
if display.responds(to: applySel) {
    let applyMethod = class_getMethodImplementation(displayClass, applySel)
    let applyFn = unsafeBitCast(applyMethod, to: ApplyFunc.self)
    _ = applyFn(display, applySel, settings)
} else {
    let applySel2 = NSSelectorFromString("apply:")
    if display.responds(to: applySel2) {
        let applyMethod = class_getMethodImplementation(displayClass, applySel2)
        let applyFn = unsafeBitCast(applyMethod, to: ApplyFunc.self)
        _ = applyFn(display, applySel2, settings)
    }
}

// Un-mirror and place as extended display to the right of primary monitor
var configRef: CGDisplayConfigRef?
if CGBeginDisplayConfiguration(&configRef) == .success {
    CGConfigureDisplayMirrorOfDisplay(configRef, displayID, kCGNullDirectDisplay)
    let mainBounds = CGDisplayBounds(CGMainDisplayID())
    let secondaryX = Int32(mainBounds.origin.x + mainBounds.width)
    let secondaryY = Int32(mainBounds.origin.y)
    CGConfigureDisplayOrigin(configRef, displayID, secondaryX, secondaryY)
    _ = CGCompleteDisplayConfiguration(configRef, .permanently)
}

// Print ready signal for Python agent
print("READY: DISPLAY_ID=\(displayID) WIDTH=\(width) HEIGHT=\(height) FPS=\(Int(fps))")
fflush(stdout)

// Set up signal handler for clean shutdown
signal(SIGINT) { _ in
    exit(0)
}
signal(SIGTERM) { _ in
    exit(0)
}

// Run runloop / wait on stdin
let stdIn = FileHandle.standardInput
let _ = stdIn.readData(ofLength: 1)
exit(0)
