package com.adva.netemu.northbound;

// import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

// import jep.JepException;

import com.adva.netemu.NetEmu;
import com.adva.netemu.YangPool;
import com.adva.netemu.service.EmuPythonService;


public class PythonKernelService extends EmuPythonService<PythonKernelService.Settings> {

    public static class Settings implements EmuPythonService.Settings<PythonKernelService> {

        @Nonnull @Override
        public Optional<NetEmu.RegisteredService<PythonKernelService, ?>> getRegisteredService() {
            return Optional.empty();
        }
    }

    public PythonKernelService(@Nonnull final YangPool pool, @Nonnull final Settings settings) {
        super(pool, settings);
    }

    /*
    @Override
    protected void launchAppFrom(@Nonnull final EmuPythonService.Python python) throws JepException {
        Objects.requireNonNull(python, "Missing Python interpreter for launching app").launchKernelApp();
    }
    */
}
