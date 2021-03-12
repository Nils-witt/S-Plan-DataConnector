/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan.dataModels;

public class Config {
    private String bearer;
    private String url;
    private Boolean trayNotifications = false;

    public String getBearer() {
        return bearer;
    }

    public String getUrl() {
        return url;
    }

    public Boolean getTrayNotifications() {
        return trayNotifications;
    }
}
