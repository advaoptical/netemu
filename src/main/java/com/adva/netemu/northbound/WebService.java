package com.adva.netemu.northbound;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

import io.jooby.Jooby;

import com.adva.netemu.YangPool;
import com.adva.netemu.service.EmuService;


public class WebService extends EmuService {

    /*
    @Path("/yang")
    public static class Yang {

        @GET
        public String serveYang() {
            return "ADVA :: NETEMU :: YANG";
        }
    }
    */

    private static class App extends Jooby implements Supplier<Jooby> {

        @Nonnull
        private final YangPool yangPool;

        public App(@Nonnull final YangPool pool) {
            this.yangPool = pool;

            // this.mvc(Yang.class);
            this.get("/yang", context -> "ADVA :: NETEMU :: YANG");
        }

        @Nonnull @Override
        public Jooby get() {
            return this;
        }
    }

    public WebService(@Nonnull final YangPool pool) {
        super(pool);
    }

    @Override
    public void run() {
        Jooby.runApp(new String[]{}, new App(this.yangPool()));
    }
}