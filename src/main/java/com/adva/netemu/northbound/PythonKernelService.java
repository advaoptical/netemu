package com.adva.netemu.northbound;

import javax.annotation.Nonnull;

import jep.JepException;

import com.adva.netemu.YangPool;
import com.adva.netemu.service.EmuPythonService;


public class PythonKernelService extends EmuPythonService {

    public PythonKernelService(@Nonnull final YangPool pool) {
        super(pool);
    }

    @Override
    protected void launchAppFrom(@Nonnull final EmuPythonService.Python python) throws JepException {
        python.launchKernelApp();
    }
}
