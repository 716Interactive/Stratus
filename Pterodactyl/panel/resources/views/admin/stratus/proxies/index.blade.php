@extends('layouts.admin')

@section('title')
    Stratus Proxies
@endsection

@section('content-header')
    <h1>Stratus Proxies<small>Manage network entry points and proxy groups.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li class="active">Stratus</li>
        <li class="active">Proxies</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-xs-12">
        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Registered Proxies</h3>
                <div class="box-tools">
                    <button class="btn btn-sm btn-primary" data-toggle="modal" data-target="#newProxyModal">Register New Proxy</button>
                </div>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <tbody>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Host</th>
                            <th>Port</th>
                            <th>Status</th>
                            <th>Main Proxy</th>
                            <th>Type</th>
                        </tr>
                        @foreach($proxies as $proxy)
                            <tr>
                                <td><code>{{ $proxy['id'] }}</code></td>
                                <td>{{ $proxy['name'] }}</td>
                                <td>{{ $proxy['host'] }}</td>
                                <td>{{ $proxy['port'] }}</td>
                                <td>
                                    @if(isset($proxy['lastHeartbeat']))
                                        <span class="label label-success">Online</span>
                                    @else
                                        <span class="label label-warning">Static</span>
                                    @endif
                                </td>
                                <td>
                                    @if($proxy['isMain'])
                                        <span class="label label-primary">YES</span>
                                    @else
                                        <span class="label label-default">NO</span>
                                    @endif
                                </td>
                                <td>{{ $proxy['isStatic'] ? 'Static' : 'Dynamic' }}</td>
                            </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="newProxyModal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <form action="{{ route('admin.stratus.proxies.create') }}" method="POST">
            @csrf
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                    <h4 class="modal-title">Register Proxy</h4>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="name">Proxy Name</label>
                        <input type="text" name="name" class="form-control" placeholder="US-Proxy-1" required>
                    </div>
                    <div class="form-group">
                        <label for="host">Host IP</label>
                        <input type="text" name="host" class="form-control" placeholder="192.168.1.10" required>
                    </div>
                    <div class="form-group">
                        <label for="port">Port</label>
                        <input type="number" name="port" class="form-control" value="25577" required>
                    </div>
                    <div class="form-group">
                        <div class="checkbox checkbox-primary">
                            <input id="is_main" name="is_main" type="checkbox">
                            <label for="is_main">Designate as Main Proxy</label>
                        </div>
                        <p class="text-muted small">Only one proxy can be designated as "Main". New registrations will unset existing main proxies.</p>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Register</button>
                </div>
            </div>
        </form>
    </div>
</div>
@endsection
