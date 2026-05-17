<?php

namespace Pterodactyl\Http\Controllers\Api\Client\Stratus;

use Pterodactyl\Http\Controllers\Api\Client\ClientApiController;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;

class ServerController extends ClientApiController
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        parent::__construct();
        $this->api = $api;
    }

    public function index(Request $request)
    {
        $groupId = $request->query('groupId');
        $servers = $this->api->getServers(['groupId' => $groupId]) ?? [];
        return response()->json($servers);
    }
}
