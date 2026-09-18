package com.sky31.gongmultiplatform.di

import com.sky31.gongmultiplatform.ui.viewModel.AcademicViewModel
import com.sky31.gongmultiplatform.ui.viewModel.ActivityViewModel
import com.sky31.gongmultiplatform.ui.viewModel.AuthViewModel
import com.sky31.gongmultiplatform.ui.viewModel.ClassroomViewModel
import com.sky31.gongmultiplatform.ui.viewModel.ConfigViewModel
import com.sky31.gongmultiplatform.ui.viewModel.MainViewModel
import com.sky31.gongmultiplatform.ui.viewModel.GongAiViewModel
import org.koin.dsl.module

val viewModelModule = module {
    single<ConfigViewModel> { ConfigViewModel() }
    single<MainViewModel> { MainViewModel() }
    single<AcademicViewModel> { AcademicViewModel() }
    single<ActivityViewModel> { ActivityViewModel() }
    single<ClassroomViewModel> { ClassroomViewModel() }
    single<GongAiViewModel> { GongAiViewModel() }
}

val authViewModelModule = module {
    single<AuthViewModel> { AuthViewModel() }
}
