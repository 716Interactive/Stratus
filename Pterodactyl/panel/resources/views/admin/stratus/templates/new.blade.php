@extends('layouts.admin')

@section('title')
    New Template
@endsection

@section('content-header')
    <h1>New Template<small>Define a new server software type.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li><a href="{{ route('admin.stratus.orchestrator') }}">Stratus</a></li>
        <li><a href="{{ route('admin.stratus.templates') }}">Templates</a></li>
        <li class="active">New</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <div class="col-md-6">
        <form action="{{ route('admin.stratus.templates.new') }}" method="POST">
            @csrf
            <div class="box box-primary">
                <div class="box-header with-border">
                    <h3 class="box-title">Template Details</h3>
                </div>
                <div class="box-body">
                    <div class="form-group">
                        <label for="name" class="control-label">Template Name</label>
                        <input type="text" name="name" id="name" class="form-control" placeholder="e.g. Bedwars-Game" />
                        <p class="text-muted small">This name is used for identification in the panel and orchestrator.</p>
                    </div>
                </div>
                <div class="box-footer">
                    <button type="submit" class="btn btn-primary pull-right">Create Template</button>
                </div>
            </div>
        </form>
    </div>
</div>
@endsection
