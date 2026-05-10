<?php

namespace Pterodactyl\Listeners\Stratus;

use Pterodactyl\Events\Server\Created;
use Pterodactyl\Contracts\Repository\ServerRepositoryInterface;
use Pterodactyl\Services\Servers\ServerControlService;
use Pterodactyl\Repositories\Wings\DaemonFileRepository;
use Illuminate\Support\Facades\Log;

class AutoInstallPluginListener
{
    protected DaemonFileRepository $fileRepository;

    public function __construct(DaemonFileRepository $fileRepository)
    {
        $this->fileRepository = $fileRepository;
    }

    public function handle(Created $event)
    {
        $server = $event->server;

        // Check if the server is part of a Stratus group
        // In our architecture, Stratus servers are created with a specific environment variable
        $isStratus = $server->variables->where('variable', 'STRATUS_TOKEN')->count() > 0;

        if (!$isStratus) {
            return;
        }

        Log::info("Auto-installing Stratus plugin for server: {$server->uuid}");

        try {
            // Path to the master plugin JAR on the Panel's local disk
            $pluginPath = storage_path('app/stratus/StratusPlugin.jar');

            if (!file_exists($pluginPath)) {
                Log::error("StratusPlugin.jar not found at $pluginPath. Skipping auto-install.");
                return;
            }

            // Upload the file to the server's plugins directory via Wings
            $this->fileRepository->setServer($server)->putContent(
                'plugins/StratusPlugin.jar',
                file_get_contents($pluginPath)
            );

            Log::info("Successfully installed Stratus plugin on server: {$server->uuid}");
        } catch (\Exception $e) {
            Log::error("Failed to auto-install Stratus plugin: " . $e->getMessage());
        }
    }
}
