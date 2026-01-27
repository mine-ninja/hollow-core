package io.github.minehollow.proxy.restart;

import java.util.List;


public record RestartData(int timeInSeconds, List<String> targetServers) {
}
