<?php

namespace Pterodactyl\Http\Controllers\Api\Client\Stratus;

use Pterodactyl\Http\Controllers\Api\Client\ClientApiController;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;

class GroupController extends ClientApiController
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        parent::__construct();
        $this->api = $api;
    }

    public function index(Request $request)
    {
        $groups = $this->api->getGroups() ?? [];
        
        // Filter by owner
        $userId = $request->user()->id;
        $filtered = array_values(array_filter($groups, function ($g) use ($userId) {
            return $g['ownerId'] === $userId;
        }));
        
        return response()->json($filtered);
    }

    public function view(Request $request, $groupId)
    {
        $data = $this->api->getGroup($groupId);
        if (!$data || $data['ownerId'] !== $request->user()->id) {
            return response()->json(['error' => 'Not Found'], 404);
        }
        
        return response()->json($data);
    }

    public function update(Request $request, $groupId)
    {
        $data = $this->api->getGroup($groupId);
        if (!$data || $data['ownerId'] !== $request->user()->id) {
            return response()->json(['error' => 'Not Found'], 404);
        }
        
        $res = $this->api->updateGroup($groupId, $request->all());
        return response()->json($res);
    }
}
