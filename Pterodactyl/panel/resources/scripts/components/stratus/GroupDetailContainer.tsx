import React, { useState, useEffect } from 'react';
import { useParams, NavLink, Switch, Route, useLocation } from 'react-router-dom';
import useSWR from 'swr';
import http from '@/api/http';
import PageContentBlock from '@/components/elements/PageContentBlock';
import Spinner from '@/components/elements/Spinner';
import SubNavigation from '@/components/elements/SubNavigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faServer, faCogs, faSave, faNetworkWired, faArrowAltCircleUp } from '@fortawesome/free-solid-svg-icons';
import FlashMessageRender from '@/components/FlashMessageRender';
import useFlash from '@/plugins/useFlash';
import { format } from 'date-fns';

interface ServerGroup {
    id: string;
    name: string;
    templateId: string;
    minServers: number;
    maxServers: number;
    targetFreeSlots: number;
    scaleDownCooldownSeconds: number;
    preferredNodeId: string | null;
    schedulingStrategy: string;
    metadata: string | null;
    ownerId: number;
}

interface ManagedServer {
    id: string;
    pterodactylId: number | null;
    nodeId: string;
    groupId: string;
    host: string;
    port: number;
    memory: number;
    disk: number;
    state: string;
    players: number;
    createdAt: string;
}

export default () => {
    const { id } = useParams<{ id: string }>();
    const location = useLocation();
    const { clearFlashes, addFlash, addError } = useFlash();
    const [saving, setSaving] = useState(false);

    // Fetch Group details
    const { data: group, error: groupError, mutate: mutateGroup } = useSWR<ServerGroup>(
        `/api/client/stratus/groups/${id}`, 
        (url) => http.get(url).then(res => res.data)
    );

    // Fetch dynamic server list under this group
    const { data: servers, error: serversError, mutate: mutateServers } = useSWR<ManagedServer[]>(
        group ? `/api/client/stratus/servers?groupId=${id}` : null,
        (url) => http.get(url).then(res => res.data),
        { refreshInterval: 5000 } // Auto-poll servers list every 5 seconds!
    );

    // Fetch available templates for dropdown
    const { data: templates } = useSWR('/api/client/stratus/templates', (url) => 
        http.get(url).then(res => res.data)
    );

    // Settings fields
    const [name, setName] = useState('');
    const [templateId, setTemplateId] = useState('');
    const [minServers, setMinServers] = useState(0);
    const [maxServers, setMaxServers] = useState(1);
    const [targetFreeSlots, setTargetFreeSlots] = useState(1);
    const [scaleDownCooldown, setScaleDownCooldown] = useState(120);
    const [metadata, setMetadata] = useState('');

    useEffect(() => {
        if (group) {
            setName(group.name);
            setTemplateId(group.templateId);
            setMinServers(group.minServers);
            setMaxServers(group.maxServers);
            setTargetFreeSlots(group.targetFreeSlots);
            setScaleDownCooldown(group.scaleDownCooldownSeconds);
            setMetadata(group.metadata ?? '');
        }
    }, [group]);

    if (groupError) return <PageContentBlock title={'Error'}><div>Failed to load server group.</div></PageContentBlock>;
    if (!group) return <PageContentBlock title={'Loading'}><Spinner centered /></PageContentBlock>;

    const handleSaveSettings = (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        clearFlashes('stratus:group-settings');

        http.put(`/api/client/stratus/groups/${id}`, {
            name,
            templateId,
            minServers,
            maxServers,
            targetFreeSlots,
            scaleDownCooldownSeconds: scaleDownCooldown,
            metadata: metadata.trim() || null
        })
        .then(() => {
            addFlash({
                type: 'success',
                message: 'Server Group configuration successfully updated!',
                key: 'stratus:group-settings'
            });
            mutateGroup();
        })
        .catch(err => {
            console.error(err);
            addError({
                message: err.message || 'Failed to update server group configuration.',
                key: 'stratus:group-settings'
            });
        })
        .then(() => setSaving(false));
    };

    return (
        <PageContentBlock title={`${group.name} - Server Group`} showFlashKey={'stratus:group'}>
            <div className={'flex flex-col'}>
                <div className={'mb-4'}>
                    <h1 className={'text-3xl font-header font-medium text-neutral-100'}>{group.name}</h1>
                    <p className={'text-xs text-neutral-400 font-mono mt-1'}>Group ID: {group.id}</p>
                </div>

                <SubNavigation>
                    <div>
                        <NavLink to={`/stratus/groups/${id}`} exact>
                            <FontAwesomeIcon icon={faServer} className={'mr-2'} />
                            Active Server Instances
                        </NavLink>
                        <NavLink to={`/stratus/groups/${id}/settings`}>
                            <FontAwesomeIcon icon={faCogs} className={'mr-2'} />
                            Group Settings
                        </NavLink>
                    </div>
                </SubNavigation>

                <div className={'mt-4'}>
                    <Switch location={location}>
                        <Route path={'/stratus/groups/:id'} exact>
                            <div className={'bg-neutral-900 rounded shadow-lg border border-neutral-700 overflow-hidden'}>
                                <div className={'p-4 bg-neutral-800 border-b border-neutral-700 flex justify-between items-center'}>
                                    <h3 className={'text-lg font-header text-neutral-200'}>Dynamic Instances Pool</h3>
                                    <div className={'text-xs text-neutral-400'}>
                                        Auto-refreshing every 5s • {servers ? servers.length : 0} instances active
                                    </div>
                                </div>

                                {!servers ? (
                                    <div className={'p-20'}><Spinner centered /></div>
                                ) : (
                                    <div className={'flex flex-col'}>
                                        <div className={'grid grid-cols-12 p-3 text-xs uppercase font-bold text-neutral-400 border-b border-neutral-800 bg-neutral-900/50'}>
                                            <div className={'col-span-3'}>Server ID</div>
                                            <div className={'col-span-2'}>Allocation IP / Port</div>
                                            <div className={'col-span-2'}>Memory / Disk</div>
                                            <div className={'col-span-2 text-center'}>Status</div>
                                            <div className={'col-span-1 text-center'}>Players</div>
                                            <div className={'col-span-2 text-right'}>Created At</div>
                                        </div>

                                        {servers.length === 0 && (
                                            <div className={'p-10 text-center text-neutral-500 italic bg-neutral-900/20'}>
                                                No active server instances are currently running for this group.
                                            </div>
                                        )}

                                        {servers.map(server => (
                                            <div key={server.id} className={'grid grid-cols-12 p-3 text-sm items-center border-b border-neutral-800 hover:bg-neutral-800 transition-colors'}>
                                                <div className={'col-span-3 truncate pr-4'}>
                                                    <code className={'text-xs text-neutral-400 font-mono'}>{server.id}</code>
                                                </div>
                                                <div className={'col-span-2 font-mono text-cyan-400 text-xs'}>
                                                    {server.host}:{server.port}
                                                </div>
                                                <div className={'col-span-2 text-xs text-neutral-300'}>
                                                    {server.memory} MB / {server.disk} MB
                                                </div>
                                                <div className={'col-span-2 text-center'}>
                                                    <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${
                                                        server.state === 'READY' ? 'bg-green-900/60 text-green-300 border border-green-700/50' :
                                                        server.state === 'IN_GAME' ? 'bg-cyan-900/60 text-cyan-300 border border-cyan-700/50' :
                                                        server.state === 'PREPARING' ? 'bg-yellow-900/60 text-yellow-300 border border-yellow-700/50' :
                                                        'bg-neutral-800 text-neutral-400'
                                                    }`}>
                                                        {server.state}
                                                    </span>
                                                </div>
                                                <div className={'col-span-1 text-center font-bold text-neutral-200'}>
                                                    {server.players}
                                                </div>
                                                <div className={'col-span-2 text-right text-xs text-neutral-500'}>
                                                    {format(new Date(server.createdAt), 'MMM do, yyyy HH:mm')}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </Route>

                        <Route path={'/stratus/groups/:id/settings'} exact>
                            <form onSubmit={handleSaveSettings} className={'bg-neutral-900 p-6 rounded shadow-lg border border-neutral-700'}>
                                <h3 className={'text-xl font-header text-neutral-200 mb-2'}>Edit Group Configuration</h3>
                                <p className={'text-neutral-400 text-sm mb-6'}>
                                    Configure auto-scaling triggers, player routing metrics, and default parameters for dynamic server group provisioning.
                                </p>
                                
                                <FlashMessageRender byKey={'stratus:group-settings'} className={'mb-4'} />

                                <div className={'grid grid-cols-1 md:grid-cols-2 gap-6'}>
                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Group Name</label>
                                        <input
                                            type={'text'}
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>

                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Base Template</label>
                                        <select
                                            value={templateId}
                                            onChange={(e) => setTemplateId(e.target.value)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2.5 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        >
                                            <option value="" disabled>Select a Template</option>
                                            {templates?.map((t: any) => (
                                                <option key={t.id} value={t.id}>{t.name}</option>
                                            ))}
                                        </select>
                                    </div>

                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Minimum Server Instances</label>
                                        <input
                                            type={'number'}
                                            value={minServers}
                                            onChange={(e) => setMinServers(parseInt(e.target.value) || 0)}
                                            required
                                            min={0}
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>
                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Maximum Server Instances</label>
                                        <input
                                            type={'number'}
                                            value={maxServers}
                                            onChange={(e) => setMaxServers(parseInt(e.target.value) || 1)}
                                            required
                                            min={1}
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>

                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Target Free Slots</label>
                                        <input
                                            type={'number'}
                                            value={targetFreeSlots}
                                            onChange={(e) => setTargetFreeSlots(parseInt(e.target.value) || 0)}
                                            required
                                            min={0}
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>
                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Scale-down Cooldown (Seconds)</label>
                                        <input
                                            type={'number'}
                                            value={scaleDownCooldown}
                                            onChange={(e) => setScaleDownCooldown(parseInt(e.target.value) || 120)}
                                            required
                                            min={10}
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>JSON Metadata / Routing Tags</label>
                                        <textarea
                                            value={metadata}
                                            onChange={(e) => setMetadata(e.target.value)}
                                            rows={4}
                                            placeholder={'{"type": "competitive-pvp", "map": "desert-arena"}'}
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500 font-mono text-sm'}
                                        />
                                    </div>
                                </div>

                                <div className={'flex justify-end mt-8 pt-4 border-t border-neutral-800'}>
                                    <button
                                        type={'submit'}
                                        disabled={saving}
                                        className={'bg-cyan-600 hover:bg-cyan-500 disabled:bg-cyan-800 text-white px-5 py-2.5 rounded font-medium transition-colors text-sm flex items-center space-x-2'}
                                    >
                                        {saving ? <Spinner size={'small'} /> : <FontAwesomeIcon icon={faSave} />}
                                        <span>{saving ? 'Saving...' : 'Save Settings'}</span>
                                    </button>
                                </div>
                            </form>
                        </Route>
                    </Switch>
                </div>
            </div>
        </PageContentBlock>
    );
};
