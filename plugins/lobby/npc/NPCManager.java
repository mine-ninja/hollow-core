// ...existing code...
    private boolean lookAtPlayerEnabled = false;
    private int lookTaskId = -1;
// ...existing code...
    public void setLookAtPlayerEnabled(boolean enabled) {
        this.lookAtPlayerEnabled = enabled;
        if (enabled) {
            startLookTask();
        } else {
            stopLookTask();
        }
    }
    public boolean isLookAtPlayerEnabled() {
        return lookAtPlayerEnabled;
    }
    private void startLookTask() {
        if (lookTaskId != -1) return;
        lookTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateNpcLook, 5L, 5L).getTaskId();
    }
    private void stopLookTask() {
        if (lookTaskId != -1) {
            Bukkit.getScheduler().cancelTask(lookTaskId);
            lookTaskId = -1;
        }
    }
    private void updateNpcLook() {
        if (!lookAtPlayerEnabled) return;
        for (var handler : npcs.values()) {
            Player nearest = null;
            double minDist = 8.0 * 8.0; // 8 blocks squared
            var npcLoc = handler.getData().location();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(npcLoc.getWorld())) continue;
                double dist = player.getLocation().distanceSquared(npcLoc);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = player;
                }
            }
            if (nearest != null) {
                handler.lookAt(nearest.getLocation());
            }
        }
    }
// ...existing code...
