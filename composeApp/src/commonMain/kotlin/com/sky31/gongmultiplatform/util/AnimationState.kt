package com.sky31.gongmultiplatform.util

sealed class AnimationState {
    data object Unstarted : AnimationState()
    data object Loading : AnimationState()
    data object Finished : AnimationState()
}