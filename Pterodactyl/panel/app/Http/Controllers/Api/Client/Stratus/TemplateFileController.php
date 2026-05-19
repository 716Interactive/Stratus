<?php

namespace Pterodactyl\Http\Controllers\Api\Client\Stratus;

use Pterodactyl\Http\Controllers\Api\Client\ClientApiController;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class TemplateFileController extends ClientApiController
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        parent::__construct();
        $this->api = $api;
    }

    protected function validateTemplateOwnership(string $templateId, int $userId)
    {
        $template = $this->api->getTemplate($templateId);
        if (!$template || $template['template']['ownerId'] !== $userId) {
            throw new NotFoundHttpException('Template not found or access denied.');
        }
        return $template;
    }

    public function list(Request $request, $templateId)
    {
        $this->validateTemplateOwnership($templateId, $request->user()->id);
        
        $directory = $request->query('directory', '/');
        $files = $this->api->get("/templates/{$templateId}/files/list", ['directory' => $directory]);
        
        return response()->json($files ?? []);
    }

    public function contents(Request $request, $templateId)
    {
        $this->validateTemplateOwnership($templateId, $request->user()->id);
        
        $file = $request->query('file');
        $contents = $this->api->get("/templates/{$templateId}/files/contents", ['file' => $file]);
        
        return response($contents)->header('Content-Type', 'text/plain');
    }

    public function write(Request $request, $templateId)
    {
        $this->validateTemplateOwnership($templateId, $request->user()->id);
        
        $file = $request->query('file');
        $content = $request->getContent();
        
        $this->api->postRaw("/templates/{$templateId}/files/write?file=" . urlencode($file), $content);
        
        return response()->noContent();
    }

    public function delete(Request $request, $templateId)
    {
        $this->validateTemplateOwnership($templateId, $request->user()->id);
        
        $files = $request->input('files', []);
        $this->api->post("/templates/{$templateId}/files/delete", $files);
        
        return response()->noContent();
    }

    public function upload(Request $request, $templateId)
    {
        $this->validateTemplateOwnership($templateId, $request->user()->id);
        
        if (!$request->hasFile('file')) {
            return response()->json(['error' => 'No file uploaded'], 400);
        }
        
        $file = $request->file('file');
        $directory = $request->input('directory', '/');
        $extract = $request->input('extract', 'false');
        
        $multipart = [
            [
                'name'     => 'file',
                'contents' => fopen($file->getRealPath(), 'r'),
                'filename' => $file->getClientOriginalName()
            ]
        ];
        
        $this->api->multipart("/templates/{$templateId}/files/upload", $multipart, [
            'directory' => $directory,
            'extract' => $extract
        ]);
        
        return response()->noContent();
     }

     public function rename(Request $request, $templateId)
     {
         $this->validateTemplateOwnership($templateId, $request->user()->id);
         
         $from = $request->input('from');
         $to = $request->input('to');
         
         $this->api->post("/templates/{$templateId}/files/rename", [
             'from' => $from,
             'to' => $to
         ]);
         
         return response()->noContent();
     }

     public function compress(Request $request, $templateId)
     {
         $this->validateTemplateOwnership($templateId, $request->user()->id);
         
         $directory = $request->input('directory', '/');
         $files = $request->input('files', []);
         
         $res = $this->api->post("/templates/{$templateId}/files/compress", [
             'directory' => $directory,
             'files' => $files
         ]);
         
         return response()->json($res ?? []);
     }

     public function decompress(Request $request, $templateId)
     {
         $this->validateTemplateOwnership($templateId, $request->user()->id);
         
         $file = $request->input('file');
         
         $this->api->post("/templates/{$templateId}/files/decompress", [
             'file' => $file
         ]);
         
         return response()->noContent();
     }

     public function download(Request $request, $templateId)
     {
         $this->validateTemplateOwnership($templateId, $request->user()->id);
         
         $file = $request->query('file');
         $content = $this->api->get("/templates/{$templateId}/files/download", ['file' => $file]);
         
         return response($content)
             ->header('Content-Type', 'application/octet-stream')
             ->header('Content-Disposition', 'attachment; filename="' . basename($file) . '"');
     }
}
