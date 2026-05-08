@extends('layouts.admin')

@section('title')
    Stratus Templates
@endsection

@section('content-header')
    <h1>Server Templates<small>Manage software versions and egg configs.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li><a href="{{ route('admin.stratus.orchestrator') }}">Stratus</a></li>
        <li class="active">Templates</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-xs-12">
        <div class="box box-primary">
            <div class="box-header with-border">
                <h3 class="box-title">Configured Templates</h3>
                <div class="box-tools">
                    <a href="{{ route('admin.stratus.templates.new') }}" class="btn btn-sm btn-primary">Create New</a>
                </div>
            </div>
            <div class="box-body table-responsive no-padding">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Current Version</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach($templates as $template)
                            <tr>
                                <td><code>{{ $template['id'] }}</code></td>
                                <td><a href="{{ route('admin.stratus.templates.view', $template['id']) }}">{{ $template['name'] }}</a></td>
                                <td>
                                    @if($template['currentVersionId'])
                                        <code>{{ $template['currentVersionId'] }}</code>
                                    @else
                                        <span class="label label-warning">No Version</span>
                                    @endif
                                </td>
                                <td class="text-right">
                                    <a href="{{ route('admin.stratus.templates.view', $template['id']) }}" class="btn btn-xs btn-default"><i class="fa fa-eye"></i> Manage Versions</a>
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
