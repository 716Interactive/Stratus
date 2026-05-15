<?php

namespace Pterodactyl\Services\Stratus;

use GuzzleHttp\Client;
use Illuminate\Support\Facades\Log;

class StratusApiService
{
    protected Client $client;

    public function __construct()
    {
        $this->client = new Client([
            'base_uri' => config('stratus.url'),
            'headers' => [
                'Authorization' => 'Bearer ' . config('stratus.token'),
                'Accept' => 'application/json',
                'Content-Type' => 'application/json',
            ],
            'timeout' => 5.0,
        ]);
    }

    public function getServers(array $params = [])
    {
        return $this->get('/servers', $params);
    }

    public function getGroups()
    {
        return $this->get('/groups');
    }

    public function getTemplates()
    {
        return $this->get('/templates');
    }

    public function getTemplate(string $id)
    {
        return $this->get('/templates/' . $id);
    }

    public function createTemplate(array $data)
    {
        return $this->post('/templates', $data);
    }

    public function createTemplateVersion(string $templateId, array $data)
    {
        return $this->post('/templates/' . $templateId . '/versions', $data);
    }

    public function getGroup(string $id)
    {
        return $this->get('/groups/' . $id);
    }

    public function createGroup(array $data)
    {
        return $this->post('/groups', $data);
    }

    public function updateGroup(string $id, array $data)
    {
        return $this->put('/groups/' . $id, $data);
    }

    public function getProxies()
    {
        return $this->get('/proxies');
    }

    public function getMainProxy()
    {
        return $this->get('/proxies/main');
    }

    public function createProxy(array $data)
    {
        return $this->post('/proxies', $data);
    }

    public function getHealth()
    {
        return $this->get('/health');
    }

    public function get(string $endpoint, array $query = [])
    {
        try {
            $response = $this->client->get($endpoint, ['query' => $query]);
            return json_decode($response->getBody()->getContents(), true);
        } catch (\Exception $e) {
            Log::error('Stratus API Error (GET ' . $endpoint . '): ' . $e->getMessage());
            return null;
        }
    }

    public function post(string $endpoint, array $data = [])
    {
        try {
            $response = $this->client->post($endpoint, ['json' => $data]);
            return json_decode($response->getBody()->getContents(), true);
        } catch (\Exception $e) {
            Log::error('Stratus API Error (POST ' . $endpoint . '): ' . $e->getMessage());
            return null;
        }
    }

    public function put(string $endpoint, array $data = [])
    {
        try {
            $response = $this->client->put($endpoint, ['json' => $data]);
            return json_decode($response->getBody()->getContents(), true);
        } catch (\Exception $e) {
            Log::error('Stratus API Error (PUT ' . $endpoint . '): ' . $e->getMessage());
            return null;
        }
    }
}
