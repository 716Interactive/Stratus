@extends('layouts.admin')

@section('title')
    Stratus Orchestrator
@endsection

@section('content-header')
    <h1>Stratus Orchestrator<small>Central management for network scaling and automation.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li class="active">Stratus</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-md-3">
        <div class="info-box">
            <span class="info-box-icon bg-blue"><i class="fa fa-server"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Total Servers</span>
                <span class="info-box-number">{{ count($servers) }}</span>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="info-box">
            <span class="info-box-icon bg-green"><i class="fa fa-cubes"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Active Groups</span>
                <span class="info-box-number">{{ count($groups) }}</span>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="info-box">
            <span class="info-box-icon bg-yellow"><i class="fa fa-file-code-o"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Templates</span>
                <span class="info-box-number">{{ count($templates) }}</span>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="info-box">
            <span class="info-box-icon bg-red"><i class="fa fa-shield"></i></span>
            <div class="info-box-content">
                <span class="info-box-text">Proxies</span>
                <span class="info-box-number">{{ count($proxies) }}</span>
            </div>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Recent Scaling Audit Logs</h3>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Level</th>
                            <th>Category</th>
                            <th>Message</th>
                            <th>Time</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($auditLogs as $log)
                            <tr>
                                <td>
                                    @if($log['level'] === 'ERROR')
                                        <span class="label label-danger">ERROR</span>
                                    @elseif($log['level'] === 'WARNING')
                                        <span class="label label-warning">WARN</span>
                                    @else
                                        <span class="label label-info">{{ $log['level'] }}</span>
                                    @endif
                                </td>
                                <td><code>{{ $log['category'] }}</code></td>
                                <td>{{ $log['message'] }}</td>
                                <td>{{ \Carbon\Carbon::parse($log['createdAt'])->diffForHumans() }}</td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="box box-solid box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Quick Actions</h3>
            </div>
            <div class="box-body">
                <a href="{{ route('admin.stratus.groups.new') }}" class="btn btn-block btn-social btn-success">
                    <i class="fa fa-plus"></i> Create New Server Group
                </a>
                <a href="{{ route('admin.stratus.templates.new') }}" class="btn btn-block btn-social btn-primary">
                    <i class="fa fa-code-fork"></i> Create New Template
                </a>
                <a href="{{ route('admin.stratus.proxies') }}" class="btn btn-block btn-social btn-warning">
                    <i class="fa fa-random"></i> Manage Proxy Network
                </a>
            </div>
        </div>
        
        <div class="box box-solid box-info">
            <div class="box-header with-border">
                <h3 class="box-title">Google Drive Backup</h3>
            </div>
            <div class="box-body">
                @if($driveConfig && $driveConfig['accessToken'])
                    <p class="text-success"><i class="fa fa-check-circle"></i> Connected to Google Drive</p>
                    <p class="small text-muted">Last Backup: {{ $driveConfig['lastBackupAt'] ?? 'Never' }}</p>
                    <button class="btn btn-xs btn-default btn-block">Trigger Manual Backup</button>
                @else
                    <p class="text-warning"><i class="fa fa-exclamation-triangle"></i> Not Connected</p>
                    <a href="{{ route('admin.stratus.backups.setup') }}" class="btn btn-sm btn-info btn-block">Connect Google Account</a>
                @endif
            </div>
        </div>
    </div>
</div>
@endsection
