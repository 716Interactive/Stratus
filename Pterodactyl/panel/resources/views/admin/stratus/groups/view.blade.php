@extends('layouts.admin')

@section('title')
    Manage Group: {{ $group['name'] }}
@endsection

@section('content-header')
    <h1>{{ $group['name'] }}<small>Group dashboard and settings.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li><a href="{{ route('admin.stratus.orchestrator') }}">Stratus</a></li>
        <li><a href="{{ route('admin.stratus.groups') }}">Groups</a></li>
        <li class="active">{{ $group['name'] }}</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-md-4">
        <form action="{{ route('admin.stratus.groups.view', $group['id']) }}" method="POST">
            @csrf
            <div class="box box-primary">
                <div class="box-header with-border">
                    <h3 class="box-title">Group Settings</h3>
                </div>
                <div class="box-body">
                    <div class="form-group">
                        <label for="name" class="control-label">Name</label>
                        <input type="text" name="name" id="name" class="form-control" value="{{ $group['name'] }}" disabled />
                    </div>
                    <div class="form-group">
                        <label for="templateId" class="control-label">Template</label>
                        <select name="templateId" id="templateId" class="form-control">
                            @foreach($templates as $template)
                                <option value="{{ $template['id'] }}" {{ $group['templateId'] === $template['id'] ? 'selected' : '' }}>
                                    {{ $template['name'] }}
                                </option>
                            @endforeach
                        </select>
                    </div>
                    <hr />
                    <div class="row">
                        <div class="form-group col-md-6">
                            <label for="minServers" class="control-label">Min Servers</label>
                            <input type="number" name="minServers" id="minServers" class="form-control" value="{{ $group['minServers'] }}" />
                        </div>
                        <div class="form-group col-md-6">
                            <label for="maxServers" class="control-label">Max Servers</label>
                            <input type="number" name="maxServers" id="maxServers" class="form-control" value="{{ $group['maxServers'] }}" />
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="targetFreeSlots" class="control-label">Target Free Slots</label>
                        <input type="number" name="targetFreeSlots" id="targetFreeSlots" class="form-control" value="{{ $group['targetFreeSlots'] }}" />
                    </div>
                    <div class="form-group">
                        <label for="scaleDownCooldownSeconds" class="control-label">Cooldown (Seconds)</label>
                        <input type="number" name="scaleDownCooldownSeconds" id="scaleDownCooldownSeconds" class="form-control" value="{{ $group['scaleDownCooldownSeconds'] }}" />
                    </div>
                </div>
                <div class="box-footer">
                    <button type="submit" class="btn btn-primary pull-right">Save Changes</button>
                </div>
            </div>
        </form>
    </div>

    <div class="col-md-8">
        <div class="row">
            <div class="col-md-6">
                <div class="small-box bg-aqua">
                    <div class="inner">
                        <h3>{{ count($servers) }}</h3>
                        <p>Total Servers</p>
                    </div>
                    <div class="icon"><i class="fa fa-server"></i></div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="small-box bg-green">
                    <div class="inner">
                        <h3>{{ count(array_filter($servers, fn($s) => $s['state'] === 'READY')) }}</h3>
                        <p>Ready Capacity</p>
                    </div>
                    <div class="icon"><i class="fa fa-check"></i></div>
                </div>
            </div>
        </div>

        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Group Servers</h3>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>State</th>
                            <th>Connection</th>
                            <th>Players</th>
                            <th>Version</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($servers as $server)
                            <tr>
                                <td><code>{{ substr($server['id'], 0, 8) }}</code></td>
                                <td>
                                    @switch($server['state'])
                                        @case('READY') <span class="label label-success">READY</span> @break
                                        @case('STARTING') <span class="label label-warning">STARTING</span> @break
                                        @case('IN_GAME') <span class="label label-info">IN_GAME</span> @break
                                        @default <span class="label label-default">{{ $server['state'] }}</span>
                                    @endswitch
                                </td>
                                <td><code>{{ $server['host'] }}:{{ $server['port'] }}</code></td>
                                <td>{{ $server['players'] }}</td>
                                <td><code>{{ substr($server['templateVersionId'], 0, 8) }}</code></td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
@endsection
