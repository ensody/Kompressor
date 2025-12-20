const std = @import("std");

pub fn build(b: *std.Build) !void {
    // The Windows builds create a .lib file in the lib/ directory which we don't need.
    const deleteLib = b.addRemoveDirTree(.{ .cwd_relative = b.getInstallPath(.prefix, "lib") });
    b.getInstallStep().dependOn(&deleteLib.step);

    try setupTarget(b, &deleteLib.step, .linux, .aarch64, "linuxArm64");
    try setupTarget(b, &deleteLib.step, .linux, .x86_64, "linuxX64");
    try setupTarget(b, &deleteLib.step, .macos, .aarch64, "macosArm64");
    try setupTarget(b, &deleteLib.step, .macos, .x86_64, "macosX64");
    try setupTarget(b, &deleteLib.step, .windows, .x86_64, "mingwX64");
}

fn setupTarget(b: *std.Build, step: *std.Build.Step, comptime tag: std.Target.Os.Tag, comptime arch: std.Target.Cpu.Arch, comptime kmpTarget: []const u8) !void {
    const libPrefix = switch (tag) {
        .windows => "lib",
        else => "",
    };
    const lib = b.addLibrary(.{ .name = libPrefix ++ "zstd-jni", .linkage = .dynamic, .root_module = b.createModule(.{
        .target = b.resolveTargetQuery(.{ .os_tag = tag, .cpu_arch = arch }),
    }) });

    lib.root_module.addIncludePath(b.path("../../../jni/include/share"));
    lib.root_module.addIncludePath(b.path("../../../jni/include/" ++ switch (tag) {
        .windows => "windows",
        else => "unix",
    }));
    lib.root_module.addIncludePath(b.path("../../build/nativebuilds/zstd-headers-iosarm64/include"));

    lib.root_module.link_libc = true;
    lib.root_module.link_libcpp = true;

    lib.root_module.addLibraryPath(b.path("../../build/nativebuilds/zstd-libzstd-jvm/jni/" ++ kmpTarget));
    lib.root_module.linkSystemLibrary(libPrefix ++ "zstd", .{ .use_pkg_config = .no });

    // TODO: Automatically find all .c/.cpp files in the jni folder
    lib.root_module.addCSourceFiles(.{
        .files = &.{
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
