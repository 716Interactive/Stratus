@extends('layouts.admin')

@section('title')
    Manage Template: {{ $template['name'] }}
@endsection

@section('content-header')
    <h1>{{ $template['name'] }}<small>Manage versions and configuration.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li><a href="{{ route('admin.stratus.orchestrator') }}">Stratus</a></li>
        <li><a href="{{ route('admin.stratus.templates') }}">Templates</a></li>
        <li class="active">{{ $template['name'] }}</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-md-8">
        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Version History</h3>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Ver</th>
                            <th>ID</th>
                            <th>Egg ID</th>
                            <th>Created</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($versions as $version)
                            <tr>
                                <td><span class="badge bg-blue">v{{ $version['versionNumber'] }}</span></td>
                                <td><code>{{ $version['id'] }}</code></td>
                                <td>{{ $version['eggId'] }}</td>
                                <td>{{ \Carbon\Carbon::parse($version['createdAt'])->diffForHumans() }}</td>
                                <td>
                                    @if($template['currentVersionId'] === $version['id'])
                                        <span class="label label-success">CURRENT</span>
                                    @else
                                        <span class="label label-default">OLD</span>
                                    @endif
                                </td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <form action="{{ route('admin.stratus.templates.view', $template['id']) }}/versions" method="POST">
            @csrf
            <div class="box box-success">
                <div class="box-header with-border">
                    <h3 class="box-title">Create New Version</h3>
                </div>
                <div class="box-body">
                    <div class="form-group">
                        <label for="eggId" class="control-label">Pterodactyl Egg ID</label>
                        <input type="number" name="eggId" id="eggId" class="form-control" placeholder="1" />
                    </div>
                    <div class="form-group">
                        <label for="config" class="control-label">Configuration JSON</label>
                        <textarea name="config" id="config" rows="10" class="form-control" style="font-family: monospace;">{
  "startup": "java -Xms128M -Xmx2048M -jar server.jar",
  "image": "ghcr.io/pterodactyl/yolks:java_17",
  "env": {
    "VERSION": "latest"
  }
}</textarea>
                    </div>
                </div>
                <div class="box-footer">
                    <p class="text-muted small">Creating a new version will automatically update the <strong>current version</strong> pointer. New servers will use this version immediately.</p>
                    <button type="submit" class="btn btn-success btn-block">Publish Version</button>
                </div>
            </div>
        </form>
    </div>
</div>
@endsection
