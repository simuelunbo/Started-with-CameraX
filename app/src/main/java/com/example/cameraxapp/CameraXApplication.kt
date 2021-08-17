package com.example.cameraxapp

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class CameraXApplication : Application(),
    CameraXConfig.Provider { // cameraXConfig provider lifetime 이 지속 되는 provider instances 에 대해 사용자 지정 가능한 옵션을 제공
    override fun getCameraXConfig(): CameraXConfig { // 인스턴스 초기화 하는데 사용할 구성
        return Camera2Config.defaultConfig()
    }
}