<?php

namespace Pterodactyl\Http\Controllers\Admin\Stratus;

use Pterodactyl\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Pterodactyl\Services\Stratus\StratusApiService;
use Illuminate\Support\Facades\URL;

class BackupController extends Controller
{
    protected StratusApiService $api;

    public function __construct(StratusApiService $api)
    {
        $this->api = $api;
    }

    public function setup()
    {
        $config = $this->api->get('/backups/config');
        return view('admin.stratus.backups.setup', ['config' => $config]);
    }

    public function redirect()
    {
        $config = $this->api->get('/backups/config');
        if (!$config) {
            return redirect()->back()->withErrors('Failed to fetch backup config.');
        }

        $query = http_build_query([
            'client_id' => $config['clientId'],
            'redirect_uri' => route('admin.stratus.backups.callback'),
            'response_type' => 'code',
            'scope' => 'https://www.googleapis.com/auth/drive.file',
            'access_type' => 'offline',
            'prompt' => 'consent',
        ]);

        return redirect('https://accounts.google.com/o/oauth2/v2/auth?' . $query);
    }

    public function callback(Request $request)
    {
        $code = $request->input('code');
        if (!$code) {
            return redirect()->route('admin.stratus.backups.setup')->withErrors('No code provided by Google.');
        }

        // Send the code to the orchestrator to exchange for tokens
        $response = $this->api->post('/backups/callback', ['code' => $code]);

        if (!$response) {
            return redirect()->route('admin.stratus.backups.setup')->withErrors('Failed to exchange code for tokens.');
        }

        return redirect()->route('admin.stratus.orchestrator')->with('success', 'Google Drive connected successfully.');
    }
}
