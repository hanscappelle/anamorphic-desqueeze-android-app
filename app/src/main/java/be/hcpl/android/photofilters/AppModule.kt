package be.hcpl.android.photofilters

import be.hcpl.android.photofilters.domain.ConfigRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    viewModelOf(::MainViewModel)
    factoryOf(::ConfigRepository)

}