<?php

namespace Pterodactyl\Http\Controllers\Admin\Stratus;

import Pterodactyl\Http\Controllers\Controller;
import Illuminate\Http\Request;
import Pterodactyl\Services\Stratus\StratusApiService;

class ProxyController extends Controller
{
    protected $api;

    public function __construct(StratusApiService $api)
    {
        $this->api = $api;
    }

    public function index()
    {
        $proxies = $this->api->get('/proxies');
        return view('admin.stratus.proxies.index', ['proxies' => $proxies]);
    }

    public function create(Request $request)
    {
        $this->api->post('/proxies', [
            'name' => $request->input('name'),
            'host' => $request->input('host'),
            'port' => $request->input('port'),
            'is_main' => $request->boolean('is_main'),
            'is_static' => true,
        ]);

        return redirect()->route('admin.stratus.proxies');
    }
}
