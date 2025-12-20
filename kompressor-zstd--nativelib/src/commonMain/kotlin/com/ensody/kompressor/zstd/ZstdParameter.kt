package com.ensody.kompressor.zstd

internal object ZstdParameter {
    const val compressionLevel: UInt = 100U
    const val windowLog: UInt = 101U
    const val hashLog: UInt = 102U
    const val chainLog: UInt = 103U
    const val searchLog: UInt = 104U
    const val minMatch: UInt = 105U
    const val targetLength: UInt = 106U
    const val strategy: UInt = 107U
    const val targetCBlockSize: UInt = 130U
    const val enableLongDistanceMatching: UInt = 160U
    const val ldmHashLog: UInt = 161U
    const val ldmMinMatch: UInt = 162U
    const val ldmBucketSizeLog: UInt = 163U
    const val ldmHashRateLog: UInt = 164U
    const val contentSizeFlag: UInt = 200U
    const val checksumFlag: UInt = 201U
    const val dictIDFlag: UInt = 202U
    const val nbWorkers: UInt = 400U
    const val jobSize: UInt = 401U
    const val overlapLog: UInt = 402U
}
