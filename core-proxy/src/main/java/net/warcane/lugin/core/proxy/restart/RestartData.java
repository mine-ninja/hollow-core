package net.warcane.lugin.core.proxy.restart;

import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 03/12/2025
 * @project LUGIN
 */
public record RestartData(int timeInSeconds, List<String> targetServers) {
}
