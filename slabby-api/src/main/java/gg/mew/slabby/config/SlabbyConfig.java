package gg.mew.slabby.config;

public interface SlabbyConfig {

    int maxStock();

    String item();

    Database database();

    Defaults defaults();

    Restock restock();

    interface Database {

        String url();

    }

    interface Defaults {

        double buyPrice();

        double sellPrice();

        int quantity();

        String note();

    }

    interface Restock {

        Punch punch();

        Chests chests();

        interface Punch {

            boolean enabled();

            boolean bulk();

            boolean shulker();

        }

        interface Chests {

            boolean enabled();

            Hoppers hoppers();

            interface Hoppers {
                boolean enabled();
                boolean batches();
            }

        }

    }

}
