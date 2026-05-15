<?php

namespace Pterodactyl\Http\Controllers\Admin\Stratus;

use Pterodactyl\Http\Controllers\Controller;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;
use Pterodactyl\Models\Nest;
use Pterodactyl\Models\Egg;

class TemplateController extends Controller
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        $this->api = $api;
    }

    public function index()
    {
        $templates = $this->api->getTemplates() ?? [];
        return view('admin.stratus.templates.index', ['templates' => $templates]);
    }

    public function view($template)
    {
        $data = $this->api->getTemplate($template);
        if (!$data) {
            return redirect()->route('admin.stratus.templates')->withErrors('Template not found.');
        }

        $nests = Nest::with('eggs')->get();
        
        return view('admin.stratus.templates.view', [
            'template' => $data['template'],
            'versions' => $data['versions'],
            'nests' => $nests,
        ]);
    }

    public function create()
    {
        return view('admin.stratus.templates.new');
    }

    public function store(Request $request)
    {
        $request->validate(['name' => 'required|string']);
        $data = $request->all();
        $data['ownerId'] = $request->user()->id;
        $template = $this->api->createTemplate($data);

        if (!$template) {
            return redirect()->back()->withErrors('Failed to create template.');
        }

        return redirect()->route('admin.stratus.templates.view', $template['id'])->with('success', 'Template created.');
    }

    public function storeVersion(Request $request, $template)
    {
        $request->validate([
            'eggId' => 'required|integer',
            'config' => 'required|string',
        ]);

        $version = $this->api->createTemplateVersion($template, $request->all());

        if (!$version) {
            return redirect()->back()->withErrors('Failed to create version.');
        }

        return redirect()->route('admin.stratus.templates.view', $template)->with('success', 'New version published successfully.');
    }
}
