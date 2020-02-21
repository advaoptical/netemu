package com.adva.netemu.service;

import javax.annotation.Nonnull;

import jep.Jep;
import jep.JepException;
import jep.MainInterpreter;
import jep.SharedInterpreter;

import com.adva.netemu.YangPool;


public class PythonService {

    @Nonnull
    private final YangPool pool;

    @Nonnull
    private final Jep jep;

    public PythonService(@Nonnull final YangPool pool) {
        this.pool = pool;
        try {
            MainInterpreter.setSharedModulesArgv("netemu");

            this.jep = new SharedInterpreter();
            this.jep.set("JAVA_NETEMU_YANG_POOL_INSTANCE", pool);
            this.jep.eval("from netemu import YangPool");
            this.jep.eval("YangPool.INSTANCE = YangPool(JAVA_NETEMU_YANG_POOL_INSTANCE)");
            this.jep.eval("__import__('ipykernel.kernelapp').kernelapp.launch_new_instance()");
            // user_ns={'pool': YangPool(pool)})");

        } catch (JepException e) {
            throw new RuntimeException(e);
        }
    }
}
