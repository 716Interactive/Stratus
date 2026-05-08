@extends('layouts.admin')

@section('title')
    New Server Group
@endsection

@section('content-header')
    <h1>New Server Group<small>Create a new managed cluster.</small></h1>
    <ol class="breadcrumb">
        <li><a href="{{ route('admin.index') }}">Admin</a></li>
        <li><a href="{{ route('admin.stratus.orchestrator') }}">Stratus</a></li>
        <li><a href="{{ route('admin.stratus.groups') }}">Groups</a></li>
        <li class="active">New</li>
    </ol>
@endsection

@section('content')
<div class="row">
    <form action="{{ route('admin.stratus.groups.new') }}" method="POST">
        @csrf
        <div class="col-md-6">
            <div class="box box-primary">
                <div class="box-header with-border">
                    <h3 class="box-title">Basic Configuration</h3>
                </div>
                <div class="box-body">
                    <div class="form-group">
                        <label for="name" class="control-label">Group Name</label>
                        <input type="text" name="name" id="name" class="form-control" placeholder="e.g. Bedwars-Lobby" />
                    </div>
                    <div class="form-group">
                        <label for="templateId" class="control-label">Server Template</label>
                        <select name="templateId" id="templateId" class="form-control">
                            @foreach($templates as $template)
                                <option value="{{ $template['id'] }}">{{ $template['name'] }} ({{ $template['id'] }})</option>
                            @endforeach
                        </select>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="box box-success">
                <div class="box-header with-border">
                    <h3 class="box-title">Scaling Parameters</h3>
                </div>
                <div class="box-body">
                    <div class="row">
                        <div class="form-group col-md-6">
                            <label for="minServers" class="control-label">Minimum Servers</label>
                            <input type="number" name="minServers" id="minServers" class="form-control" value="1" />
                        </div>
                        <div class="form-group col-md-6">
                            <label for="maxServers" class="control-label">Maximum Servers</label>
                            <input type="number" name="maxServers" id="maxServers" class="form-control" value="10" />
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="targetFreeSlots" class="control-label">Target Free Slots (Ready Capacity)</label>
                        <input type="number" name="targetFreeSlots" id="targetFreeSlots" class="form-control" value="1" />
                        <p class="text-muted small">The orchestrator will try to keep this many servers in <code>READY</code> state at all times.</p>
                    </div>
                    <div class="form-group">
                        <label for="scaleDownCooldownSeconds" class="control-label">Scale Down Cooldown (Seconds)</label>
                        <input type="number" name="scaleDownCooldownSeconds" id="scaleDownCooldownSeconds" class="form-control" value="300" />
                    </div>
                </div>
                <div class="box-footer">
                    <button type="submit" class="btn btn-success pull-right">Create Group</button>
                </div>
            </div>
        </div>
    </form>
</div>
@endsection
