<?php

namespace Pterodactyl\Http\Controllers\Admin\Stratus;

use Pterodactyl\Http\Controllers\Controller;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;

class OrchestratorController extends Controller
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        $this->api = $api;
    }

    public function index()
    {
        $health = $this->api->getHealth();
        $servers = $this->api->getServers();
        $groups = $this->api->getGroups();

        return view('admin.stratus.index', [
            'health' => $health,
            'servers' => $servers,
            'groups' => $groups,
        ]);
    }
}
