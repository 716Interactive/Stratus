@extends('layouts.admin')

@section('title')
    Stratus Server Groups
@endsection

@section('content-header')
    <h1>Server Groups<small>Manage autoscaling clusters.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li><a href="{{ route('admin.stratus.orchestrator') }}">Stratus</a></li>
        <li class="active">Groups</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-xs-12">
        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Configured Groups</h3>
                <div class="box-tools">
                    <a href="{{ route('admin.stratus.groups.new') }}" class="btn btn-sm btn-primary">Create New</a>
                </div>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Template</th>
                            <th>Min/Max</th>
                            <th>Target Slots</th>
                            <th>Cooldown</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($groups as $group)
                            <tr>
                                <td><code>{{ $group['id'] }}</code></td>
                                <td><a href="{{ route('admin.stratus.groups.view', $group['id']) }}">{{ $group['name'] }}</a></td>
                                <td><code>{{ $group['templateId'] }}</code></td>
                                <td>{{ $group['minServers'] }} / {{ $group['maxServers'] }}</td>
                                <td>{{ $group['targetFreeSlots'] }}</td>
                                <td>{{ $group['scaleDownCooldownSeconds'] }}s</td>
                                <td class="text-right">
                                    <a href="{{ route('admin.stratus.groups.view', $group['id']) }}" class="btn btn-xs btn-default"><i class="fa fa-pencil"></i></a>
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
