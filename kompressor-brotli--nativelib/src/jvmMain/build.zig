const std = @import("std");
const builtin = @import("builtin");

pub fn build(b: *std.Build) !void {
    // The Windows builds create a .lib file in the lib/ directory which we don't need.
    const deleteLib = b.addRemoveDirTree(.{ .cwd_relative = b.getInstallPath(.prefix, "lib") });
    b.getInstallStep().dependOn(&deleteLib.step);

    if (builtin.os.tag == .linux or builtin.os.tag == .macos) {
        try setupTarget(b, &deleteLib.step, .linux, .aarch64, .gnu, "linuxArm64");
        try setupTarget(b, &deleteLib.step, .linux, .x86_64, .gnu, "linuxX64");
    }
    if (builtin.os.tag == .macos) {
        try setupTarget(b, &deleteLib.step, .macos, .aarch64, null, "macosArm64");
        try setupTarget(b, &deleteLib.step, .macos, .x86_64, null, "macosX64");
    }
    if (builtin.os.tag == .windows or builtin.os.tag == .macos) {
        try setupTarget(b, &deleteLib.step, .windows, .x86_64, null, "mingwX64");
    }
}

fn setupTarget(b: *std.Build, step: *std.Build.Step, comptime tag: std.Target.Os.Tag, comptime arch: std.Target.Cpu.Arch, comptime abi: ?std.Target.Abi, comptime kmpTarget: []const u8) !void {
    const libPrefix = switch (tag) {
        .windows => "lib",
        else => "",
    };
    const libSuffix = "";
    const lib = b.addLibrary(.{ .name = libPrefix ++ "brotli-jni", .linkage = .dynamic, .root_module = b.createModule(.{ .target = b.resolveTargetQuery(.{ .os_tag = tag, .cpu_arch = arch, .abi = abi }), .optimize = .ReleaseSmall }) });

    lib.root_module.addIncludePath(b.path("../../../jni/include/share"));
    lib.root_module.addIncludePath(b.path("../../../jni/include/" ++ switch (tag) {
        .windows => "windows",
        else => "unix",
    }));
    lib.root_module.addIncludePath(b.path("../../../jni/common/include"));
    lib.root_module.addIncludePath(b.path("../../build/nativebuilds/brotli-headers-" ++ (comptime toLowerComptime(kmpTarget)) ++ "/include"));

    lib.root_module.link_libc = true;
    lib.root_module.link_libcpp = true;

    lib.root_module.addLibraryPath(b.path("../../build/nativebuilds/brotli-libbrotlicommon-jvm/jni/" ++ kmpTarget));
    lib.root_module.addLibraryPath(b.path("../../build/nativebuilds/brotli-libbrotlidec-jvm/jni/" ++ kmpTarget));
    lib.root_module.addLibraryPath(b.path("../../build/nativebuilds/brotli-libbrotlienc-jvm/jni/" ++ kmpTarget));
    lib.root_module.linkSystemLibrary(libPrefix ++ "brotlicommon" ++ libSuffix, .{ .use_pkg_config = .no });
    lib.root_module.linkSystemLibrary(libPrefix ++ "brotlidec" ++ libSuffix, .{ .use_pkg_config = .no });
    lib.root_module.linkSystemLibrary(libPrefix ++ "brotlienc" ++ libSuffix, .{ .use_pkg_config = .no });

    // TODO: Automatically find all .c/.cpp files in the jni folder
    lib.root_module.addCSourceFiles(.{
        .files = &.{
            "../../../jni/common/src/DefaultLoad.cpp",
            "../../../jni/common/src/SliceClass.cpp",
            "../jvmCommonMain/jni/Wrapper.cpp",
        },
        .flags = &.{
            "-std=c++11",
        },
    });

    const install = b.addInstallArtifact(lib, .{
        .dest_dir = .{
            .override = .{
                .custom = kmpTarget,
            },
        },
    });

    step.dependOn(&install.step);
}

fn toLowerComptime(comptime s: []const u8) []const u8 {
    comptime var buf: [s.len]u8 = undefined;

    inline for (s, 0..) |c, i| {
        buf[i] = std.ascii.toLower(c);
    }

    return &buf;
}
