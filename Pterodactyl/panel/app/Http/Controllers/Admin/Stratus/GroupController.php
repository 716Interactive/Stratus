<?php

namespace Pterodactyl\Http\Controllers\Admin\Stratus;

use Pterodactyl\Http\Controllers\Controller;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;

class GroupController extends Controller
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        $this->api = $api;
    }

    public function index()
    {
        $groups = $this->api->getGroups();
        return view('admin.stratus.groups.index', ['groups' => $groups]);
    }

    public function view($group)
    {
        $groupData = $this->api->getGroup($group);
        if (!$groupData) {
            return redirect()->route('admin.stratus.groups')->withErrors('Group not found.');
        }

        $servers = $this->api->getServers(['groupId' => $group]);
        $templates = $this->api->getTemplates();

        return view('admin.stratus.groups.view', [
            'group' => $groupData,
            'servers' => $servers,
            'templates' => $templates,
        ]);
    }

    public function create()
    {
        $templates = $this->api->getTemplates();
        $nodes = $this->api->get('/nodes'); // We need an endpoint for this
        return view('admin.stratus.groups.new', [
            'templates' => $templates,
            'nodes' => $nodes,
        ]);
    }

    public function store(Request $request)
    {
        $request->validate([
            'name' => 'required|string',
            'templateId' => 'required|string',
            'minServers' => 'required|integer|min:0',
            'maxServers' => 'required|integer|min:1',
            'targetFreeSlots' => 'required|integer|min:0',
            'scaleDownCooldownSeconds' => 'required|integer|min:0',
        ]);

        $group = $this->api->createGroup($request->all());

        if (!$group) {
            return redirect()->back()->withErrors('Failed to create group via Orchestrator API.');
        }

        return redirect()->route('admin.stratus.groups.view', $group['id'])->with('success', 'Group created successfully.');
    }

    public function update(Request $request, $group)
    {
        // TODO: implement update
        return redirect()->back();
    }
}
