<?php

namespace Pterodactyl\Http\Controllers\Admin\Stratus;

use Pterodactyl\Http\Controllers\Controller;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;

class TemplateFileController extends Controller
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        $this->api = $api;
    }

    public function list(Request $request, $template)
    {
        $directory = $request->query('directory', '/');
        $files = $this->api->get("/templates/{$template}/files/list", ['directory' => $directory]);
        
        return response()->json($files ?? []);
    }

    public function contents(Request $request, $template)
    {
        $file = $request->query('file');
        $contents = $this->api->get("/templates/{$template}/files/contents", ['file' => $file]);
        
        return response($contents)->header('Content-Type', 'text/plain');
    }

    public function write(Request $request, $template)
    {
        $file = $request->query('file');
        $content = $request->getContent();
        
        $this->api->post("/templates/{$template}/files/write?file=" . urlencode($file), [], $content);
        
        return response()->noContent();
    }

    public function delete(Request $request, $template)
    {
        $files = $request->input('files', []);
        $this->api->post("/templates/{$template}/files/delete", $files);
        
        return response()->noContent();
    }
}
