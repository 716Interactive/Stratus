@extends('layouts.admin')

@section('title')
    Stratus Live Monitor
@endsection

@section('content-header')
    <h1>Stratus Orchestrator<small>Real-time network monitoring.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li class="active">Stratus</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-md-3">
        <div class="info-box {{ $health['status'] === 'ok' ? 'bg-green' : 'bg-red' }}">
            <span class="info-box-icon"><i class="fa fa-heartbeat"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Orchestrator Status</span>
                <span class="info-box-number">{{ strtoupper($health['status'] ?? 'UNKNOWN') }}</span>
                <div class="progress">
                    <div class="progress-bar" style="width: 100%"></div>
                </div>
                <span class="progress-description">Version: {{ $health['version'] ?? 'N/A' }}</span>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="info-box bg-aqua">
            <span class="info-box-icon"><i class="fa fa-server"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Managed Servers</span>
                <span class="info-box-number">{{ count($servers) }}</span>
                <div class="progress">
                    <div class="progress-bar" style="width: 100%"></div>
                </div>
                <span class="progress-description">Across {{ count($groups) }} groups</span>
            </div>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-xs-12">
        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Active Managed Servers</h3>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Group</th>
                            <th>Node</th>
                            <th>Connection</th>
                            <th>State</th>
                            <th>Players</th>
                            <th>Last Heartbeat</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($servers as $server)
                            <tr>
                                <td><code>{{ $server['id'] }}</code></td>
                                <td><span class="label label-default">{{ $server['groupId'] }}</span></td>
                                <td>{{ $server['nodeId'] }}</td>
                                <td><code>{{ $server['host'] }}:{{ $server['port'] }}</code></td>
                                <td>
                                    @switch($server['state'])
                                        @case('READY') <span class="label label-success">READY</span> @break
                                        @case('STARTING') <span class="label label-warning">STARTING</span> @break
                                        @case('IN_GAME') <span class="label label-info">IN_GAME</span> @break
                                        @case('TERMINATED') <span class="label label-danger">TERMINATED</span> @break
                                        @default <span class="label label-default">{{ $server['state'] }}</span>
                                    @endswitch
                                </td>
                                <td>{{ $server['players'] }}</td>
                                <td>{{ \Carbon\Carbon::parse($server['lastHeartbeat'])->diffForHumans() }}</td>
                                <td>
                                    @if($server['pterodactylId'])
                                        <a href="{{ route('admin.servers.view', $server['pterodactylId']) }}" class="btn btn-xs btn-default"><i class="fa fa-external-link"></i> View Ptero</a>
                                    @endif
                                </td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
@endsection
