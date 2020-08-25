package com.adva.netemu.northbound;

import javax.annotation.Nonnull;

import jep.JepException;

import com.adva.netemu.YangPool;
import com.adva.netemu.service.EmuPythonService;


public class PythonKernelService extends EmuPythonService {

    public static class Settings implements EmuPythonService.Settings<PythonKernelService> {}

    public PythonKernelService(@Nonnull final YangPool pool, @Nonnull final Settings settings) {
        super(pool, settings);
    }

    @Override
    protected void launchAppFrom(@Nonnull final EmuPythonService.Python python) throws JepException {
        python.launchKernelApp();
    }
}
