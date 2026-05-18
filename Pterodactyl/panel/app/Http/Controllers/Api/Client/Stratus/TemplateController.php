<?php

namespace Pterodactyl\Http\Controllers\Api\Client\Stratus;

use Pterodactyl\Http\Controllers\Api\Client\ClientApiController;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;

class TemplateController extends ClientApiController
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        parent::__construct();
        $this->api = $api;
    }

    public function index(Request $request)
    {
        $templates = $this->api->getTemplates() ?? [];
        
        // Filter by owner
        $userId = $request->user()->id;
        $filtered = array_values(array_filter($templates, function ($t) use ($userId) {
            return $t['ownerId'] === $userId;
        }));
        
        return response()->json($filtered);
    }

    public function view(Request $request, $templateId)
    {
        $data = $this->api->getTemplate($templateId);
        if (!$data || $data['template']['ownerId'] !== $request->user()->id) {
            return response()->json(['error' => 'Not Found'], 404);
        }
        
        return response()->json($data);
    }

    public function update(Request $request, $templateId)
    {
        $data = $this->api->getTemplate($templateId);
        if (!$data || $data['template']['ownerId'] !== $request->user()->id) {
            return response()->json(['error' => 'Not Found'], 404);
        }
        
        $res = $this->api->put('/templates/' . $templateId, $request->all());
        return response()->json($res);
    }

    public function eggs(Request $request)
    {
        $res = $this->api->get('/templates/eggs');
        return response()->json($res);
    }
}
