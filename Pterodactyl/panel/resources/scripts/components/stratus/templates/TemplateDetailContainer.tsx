import React, { useState, useEffect } from 'react';
import { useParams, NavLink, Switch, Route, useLocation } from 'react-router-dom';
import useSWR from 'swr';
import http from '@/api/http';
import PageContentBlock from '@/components/elements/PageContentBlock';
import Spinner from '@/components/elements/Spinner';
import SubNavigation from '@/components/elements/SubNavigation';
import TemplateFileContainer from '@/components/stratus/templates/files/TemplateFileContainer';
import TemplateFileEditContainer from '@/components/stratus/templates/files/TemplateFileEditContainer';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolderOpen, faTerminal, faKey, faSave, faPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import FlashMessageRender from '@/components/FlashMessageRender';
import useFlash from '@/plugins/useFlash';

interface PteroTemplateConfig {
    memory: number;
    disk: number;
    cpu: number;
    startup: string;
    image: string;
    env: Record<string, string>;
}

export default () => {
    const { id } = useParams<{ id: string }>();
    const location = useLocation();
    const { clearFlashes, addFlash, addError } = useFlash();
    const [saving, setSaving] = useState(false);

    // Fetch Template along with versions
    const { data, error, mutate } = useSWR(`/api/client/stratus/templates/${id}`, (url) => 
        http.get(url).then(res => res.data)
    );

    // Settings fields
    const [name, setName] = useState('');
    const [eggId, setEggId] = useState(1);
    const [memory, setMemory] = useState(2048);
    const [disk, setDisk] = useState(5120);
    const [cpu, setCpu] = useState(100);
    const [startup, setStartup] = useState('');
    const [image, setImage] = useState('');
    const [envRows, setEnvRows] = useState<{ key: string; value: string }[]>([]);

    useEffect(() => {
        if (data?.template) {
            setName(data.template.name);
        }
        if (data?.versions && data.versions.length > 0) {
            const latest = data.versions[0];
            setEggId(latest.eggId);
            try {
                const config: PteroTemplateConfig = JSON.parse(latest.configJson);
                setMemory(config.memory ?? 2048);
                setDisk(config.disk ?? 5120);
                setCpu(config.cpu ?? 100);
                setStartup(config.startup ?? '');
                setImage(config.image ?? '');
                if (config.env) {
                    setEnvRows(Object.entries(config.env).map(([key, value]) => ({ key, value })));
                }
            } catch (e) {
                console.error('Failed to parse template version config JSON', e);
            }
        }
    }, [data]);

    if (error) return <PageContentBlock title={'Error'}><div>Failed to load template details.</div></PageContentBlock>;
    if (!data) return <PageContentBlock title={'Loading'}><Spinner centered /></PageContentBlock>;

    const template = data.template;

    const handleSaveSettings = (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        clearFlashes('stratus:template-settings');

        // Convert envRows back to Record<string, string>
        const env: Record<string, string> = {};
        envRows.forEach(row => {
            if (row.key.trim()) {
                env[row.key.trim()] = row.value;
            }
        });

        http.put(`/api/client/stratus/templates/${id}`, {
            name,
            eggId,
            memory,
            disk,
            cpu,
            startup,
            image,
            environment: env
        })
        .then(() => {
            addFlash({
                type: 'success',
                message: 'Template settings successfully updated!',
                key: 'stratus:template-settings'
            });
            mutate();
        })
        .catch(err => {
            console.error(err);
            addError({
                message: err.message || 'Failed to update template settings.',
                key: 'stratus:template-settings'
            });
        })
        .then(() => setSaving(false));
    };

    const addEnvRow = () => {
        setEnvRows([...envRows, { key: '', value: '' }]);
    };

    const removeEnvRow = (index: number) => {
        const updated = [...envRows];
        updated.splice(index, 1);
        setEnvRows(updated);
    };

    const handleEnvRowChange = (index: number, field: 'key' | 'value', val: string) => {
        const updated = [...envRows];
        updated[index][field] = val;
        setEnvRows(updated);
    };

    return (
        <PageContentBlock title={`${template.name} - Template`} showFlashKey={'stratus:template'}>
            <div className={'flex flex-col'}>
                <div className={'mb-4'}>
                    <h1 className={'text-3xl font-header font-medium text-neutral-100'}>{template.name}</h1>
                    <p className={'text-xs text-neutral-400 font-mono mt-1'}>Template ID: {template.id}</p>
                </div>

                <SubNavigation>
                    <div>
                        <NavLink to={`/stratus/templates/${id}`} exact>
                            <FontAwesomeIcon icon={faFolderOpen} className={'mr-2'} />
                            Files
                        </NavLink>
                        <NavLink to={`/stratus/templates/${id}/sftp`}>
                            <FontAwesomeIcon icon={faKey} className={'mr-2'} />
                            SFTP Details
                        </NavLink>
                        <NavLink to={`/stratus/templates/${id}/settings`}>
                            <FontAwesomeIcon icon={faTerminal} className={'mr-2'} />
                            Template Settings
                        </NavLink>
                    </div>
                </SubNavigation>

                <div className={'mt-4'}>
                    <Switch location={location}>
                        <Route path={'/stratus/templates/:id'} exact>
                            <TemplateFileContainer />
                        </Route>
                        <Route path={'/stratus/templates/:id/files/:action(new|edit)'} exact>
                            <TemplateFileEditContainer />
                        </Route>
                        
                        <Route path={'/stratus/templates/:id/sftp'} exact>
                            <div className={'grid grid-cols-1 lg:grid-cols-3 gap-6'}>
                                <div className={'lg:col-span-2 bg-neutral-900 p-6 rounded shadow-lg border border-neutral-700'}>
                                    <h3 className={'text-xl font-header text-neutral-200 mb-4'}>SFTP Information</h3>
                                    <p className={'text-neutral-400 text-sm mb-6'}>
                                        Connect directly to this template\'s raw golden files using any third-party SFTP client (such as FileZilla, WinSCP, or Cyberduck).
                                    </p>
                                    
                                    <div className={'grid grid-cols-1 md:grid-cols-2 gap-4'}>
                                        <div className={'bg-neutral-800 p-4 rounded border border-neutral-700/50'}>
                                            <span className={'text-xs text-neutral-400 uppercase font-bold block'}>Server Address</span>
                                            <code className={'text-cyan-400 font-mono text-sm'}>{window.location.hostname}</code>
                                        </div>
                                        <div className={'bg-neutral-800 p-4 rounded border border-neutral-700/50'}>
                                            <span className={'text-xs text-neutral-400 uppercase font-bold block'}>Port</span>
                                            <code className={'text-neutral-200 font-mono text-sm'}>2022</code>
                                        </div>
                                        <div className={'bg-neutral-800 p-4 rounded border border-neutral-700/50 md:col-span-2'}>
                                            <span className={'text-xs text-neutral-400 uppercase font-bold block'}>Username</span>
                                            <code className={'text-cyan-400 font-mono text-sm'}>template.{template.id}</code>
                                        </div>
                                        <div className={'bg-neutral-800 p-4 rounded border border-neutral-700/50 md:col-span-2'}>
                                            <span className={'text-xs text-neutral-400 uppercase font-bold block'}>Directory Path</span>
                                            <code className={'text-neutral-300 font-mono text-xs'}>/var/lib/pterodactyl/templates/{template.id}</code>
                                        </div>
                                    </div>
                                </div>
                                
                                <div className={'bg-neutral-900 p-6 rounded shadow-lg border border-neutral-700 flex flex-col justify-between'}>
                                    <div>
                                        <h3 className={'text-lg font-header text-neutral-200 mb-3'}>Password Instruction</h3>
                                        <p className={'text-neutral-400 text-sm leading-relaxed mb-4'}>
                                            To authenticate, use your **standard account password** or active API key credentials.
                                        </p>
                                        <div className={'bg-cyan-950/20 border border-cyan-800/40 p-4 rounded text-xs text-cyan-300'}>
                                            <strong>Pro Tip:</strong> Ensure that your server group is offline before making extensive bulk uploads via SFTP to avoid file locking conflicts.
                                        </div>
                                    </div>
                                    <div className={'mt-6 pt-4 border-t border-neutral-800'}>
                                        <a 
                                            href={`sftp://template.${template.id}@${window.location.hostname}:2022`} 
                                            className={'block text-center bg-cyan-600 hover:bg-cyan-500 text-white px-4 py-2.5 rounded font-medium transition-colors text-sm'}
                                        >
                                            Launch SFTP Client
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </Route>

                        <Route path={'/stratus/templates/:id/settings'} exact>
                            <form onSubmit={handleSaveSettings} className={'bg-neutral-900 p-6 rounded shadow-lg border border-neutral-700'}>
                                <h3 className={'text-xl font-header text-neutral-200 mb-2'}>Edit Template Settings</h3>
                                <p className={'text-neutral-400 text-sm mb-6'}>
                                    Configure core resource constraints and environments applied to all dynamic instances spawned from this template.
                                </p>
                                
                                <FlashMessageRender byKey={'stratus:template-settings'} className={'mb-4'} />

                                <div className={'grid grid-cols-1 md:grid-cols-2 gap-6'}>
                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Template Name</label>
                                        <input
                                            type={'text'}
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>
                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Egg ID</label>
                                        <input
                                            type={'number'}
                                            value={eggId}
                                            onChange={(e) => setEggId(parseInt(e.target.value) || 1)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>

                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Memory Limit (MB)</label>
                                        <input
                                            type={'number'}
                                            value={memory}
                                            onChange={(e) => setMemory(parseInt(e.target.value) || 2048)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>
                                    <div>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Disk Limit (MB)</label>
                                        <input
                                            type={'number'}
                                            value={disk}
                                            onChange={(e) => setDisk(parseInt(e.target.value) || 5120)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>CPU Limit (%) (0 for Unlimited)</label>
                                        <input
                                            type={'number'}
                                            value={cpu}
                                            onChange={(e) => setCpu(parseInt(e.target.value) || 0)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500'}
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Startup Command</label>
                                        <input
                                            type={'text'}
                                            value={startup}
                                            onChange={(e) => setStartup(e.target.value)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500 font-mono text-sm'}
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <label className={'block text-xs text-neutral-400 uppercase font-bold mb-2'}>Docker Image</label>
                                        <input
                                            type={'text'}
                                            value={image}
                                            onChange={(e) => setImage(e.target.value)}
                                            required
                                            className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-full border border-neutral-700 focus:outline-none focus:border-cyan-500 font-mono text-sm'}
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <div className={'flex justify-between items-center mb-3'}>
                                            <label className={'block text-xs text-neutral-400 uppercase font-bold'}>Environment Variables</label>
                                            <button 
                                                type={'button'} 
                                                onClick={addEnvRow}
                                                className={'text-cyan-500 hover:text-cyan-400 text-xs flex items-center space-x-1'}
                                            >
                                                <FontAwesomeIcon icon={faPlus} />
                                                <span>Add Variable</span>
                                            </button>
                                        </div>

                                        <div className={'space-y-2'}>
                                            {envRows.map((row, idx) => (
                                                <div key={idx} className={'flex items-center space-x-2'}>
                                                    <input
                                                        type={'text'}
                                                        value={row.key}
                                                        onChange={(e) => handleEnvRowChange(idx, 'key', e.target.value)}
                                                        placeholder={'KEY'}
                                                        className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded w-1/3 border border-neutral-700 focus:outline-none focus:border-cyan-500 font-mono text-xs'}
                                                    />
                                                    <input
                                                        type={'text'}
                                                        value={row.value}
                                                        onChange={(e) => handleEnvRowChange(idx, 'value', e.target.value)}
                                                        placeholder={'value'}
                                                        className={'bg-neutral-800 text-neutral-200 px-3 py-2 rounded flex-1 border border-neutral-700 focus:outline-none focus:border-cyan-500 font-mono text-xs'}
                                                    />
                                                    <button 
                                                        type={'button'} 
                                                        onClick={() => removeEnvRow(idx)}
                                                        className={'text-neutral-500 hover:text-red-500 p-2 transition-colors'}
                                                    >
                                                        <FontAwesomeIcon icon={faTrash} />
                                                    </button>
                                                </div>
                                            ))}
                                            {envRows.length === 0 && (
                                                <div className={'text-neutral-500 text-xs italic p-4 text-center bg-neutral-800/30 rounded border border-dashed border-neutral-700'}>
                                                    No environment variables added.
                                                </div>
                                            )}
                                        </div>
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
