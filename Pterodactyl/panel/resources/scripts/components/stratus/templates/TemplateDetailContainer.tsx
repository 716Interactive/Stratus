import React, { useState, useEffect } from 'react';
import { useParams, NavLink, Switch, Route, useLocation } from 'react-router-dom';
import useSWR from 'swr';
import http from '@/api/http';
import PageContentBlock from '@/components/elements/PageContentBlock';
import Spinner from '@/components/elements/Spinner';
import SubNavigation from '@/components/elements/SubNavigation';
import Input, { Textarea } from '@/components/elements/Input';
import Label from '@/components/elements/Label';
import Select from '@/components/elements/Select';
import TemplateFileContainer from '@/components/stratus/templates/files/TemplateFileContainer';
import TemplateFileEditContainer from '@/components/stratus/templates/files/TemplateFileEditContainer';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFolderOpen, faTerminal, faKey, faSave, faPlus, faTrash } from '@fortawesome/free-solid-svg-icons';
import FlashMessageRender from '@/components/FlashMessageRender';
import useFlash from '@/plugins/useFlash';
import { useStoreState } from 'easy-peasy';
import CopyOnClick from '@/components/elements/CopyOnClick';
import TitledGreyBox from '@/components/elements/TitledGreyBox';
import { Button } from '@/components/elements/button/index';

interface PteroTemplateConfig {
    memory: number;
    disk: number;
    cpu: number;
    startup: string;
    image: string;
    env: Record<string, string>;
}

const getEggImages = (egg: any) => {
    if (!egg || !egg.docker_images) return [];
    return Object.entries(egg.docker_images).map(([key, value]) => {
        const isImage = (str: string) => str.includes('/') || str.includes(':') || !str.includes(' ');
        const image = isImage(key) ? key : (value as string);
        const label = isImage(key) ? (value as string) : key;
        return { image, label };
    });
};

export default () => {
    const username = useStoreState((state) => state.user.data?.username || 'user');
    const { id } = useParams<{ id: string }>();
    const location = useLocation();
    const { clearFlashes, addFlash, addError } = useFlash();
    const [saving, setSaving] = useState(false);

    // Fetch Template along with versions
    const { data, error, mutate } = useSWR(`/api/client/stratus/templates/${id}`, (url) => 
        http.get(url).then(res => res.data)
    );

    // Fetch dynamic list of available Pterodactyl Eggs
    const { data: eggs } = useSWR<{ id: number; name: string }[]>('/api/client/stratus/templates/eggs', (url) =>
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
    const [useCustomImage, setUseCustomImage] = useState(false);
    const [autosaveState, setAutosaveState] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');

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
                if (config.env) {
                    setEnvRows(Object.entries(config.env).map(([key, value]) => ({ key, value })));
                }

                // Auto-detect if image is custom or needs initialization
                const selectedEgg = eggs?.find(e => e.id === latest.eggId);
                const eggImages = getEggImages(selectedEgg);
                let currentImg = config.image ?? '';
                
                const hasImage = eggImages.some(item => item.image === currentImg);
                if (currentImg && !hasImage) {
                    setUseCustomImage(true);
                } else if (!currentImg && eggImages.length > 0) {
                    currentImg = eggImages[0].image;
                }
                
                setImage(currentImg);
            } catch (e) {
                console.error('Failed to parse template version config JSON', e);
            }
        }
    }, [data, eggs]);

    const autosaveSettings = (updates: any = {}, forceEnv: { key: string; value: string }[] | null = null) => {
        setAutosaveState('saving');
        clearFlashes('stratus:template-settings');

        const targetEnvRows = forceEnv || envRows;
        const env: Record<string, string> = {};
        targetEnvRows.forEach(row => {
            if (row.key.trim()) {
                env[row.key.trim()] = row.value;
            }
        });

        const payload = {
            name: updates.hasOwnProperty('name') ? updates.name : name,
            eggId: updates.hasOwnProperty('eggId') ? updates.eggId : eggId,
            memory: updates.hasOwnProperty('memory') ? updates.memory : memory,
            disk: updates.hasOwnProperty('disk') ? updates.disk : disk,
            cpu: updates.hasOwnProperty('cpu') ? updates.cpu : cpu,
            startup: updates.hasOwnProperty('startup') ? updates.startup : startup,
            image: updates.hasOwnProperty('image') ? updates.image : image,
            environment: env
        };

        http.put(`/api/client/stratus/templates/${id}`, payload)
        .then(() => {
            setAutosaveState('saved');
            setTimeout(() => setAutosaveState(prev => prev === 'saved' ? 'idle' : prev), 2500);
            mutate();
        })
        .catch(err => {
            console.error(err);
            setAutosaveState('error');
            setTimeout(() => setAutosaveState(prev => prev === 'error' ? 'idle' : prev), 4000);
            addError({
                message: err.message || 'Failed to update template settings.',
                key: 'stratus:template-settings'
            });
        });
    };

    const addEnvRow = () => {
        const newRows = [...envRows, { key: '', value: '' }];
        setEnvRows(newRows);
    };

    const removeEnvRow = (index: number) => {
        const updated = [...envRows];
        updated.splice(index, 1);
        setEnvRows(updated);
        autosaveSettings({}, updated);
    };

    const handleEnvRowChange = (index: number, field: 'key' | 'value', val: string) => {
        const updated = [...envRows];
        updated[index][field] = val;
        setEnvRows(updated);
    };

    if (error) return <PageContentBlock title={'Error'}><div>Failed to load template details.</div></PageContentBlock>;
    if (!data) return <PageContentBlock title={'Loading'}><Spinner centered /></PageContentBlock>;

    const template = data.template;

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
                            <div className={'md:flex'}>
                                <div className={'w-full md:flex-1 md:mr-10'}>
                                    <TitledGreyBox title={'SFTP Details'}>
                                        <div>
                                            <Label>Server Address</Label>
                                            <CopyOnClick text={`sftp://${window.location.hostname}:2022`}>
                                                <Input type={'text'} value={`sftp://${window.location.hostname}:2022`} readOnly />
                                            </CopyOnClick>
                                        </div>
                                        <div className={'mt-6'}>
                                            <Label>Username</Label>
                                            <CopyOnClick text={`${username}.template.${template.id.split('-')[0]}`}>
                                                <Input type={'text'} value={`${username}.template.${template.id.split('-')[0]}`} readOnly />
                                            </CopyOnClick>
                                        </div>
                                        <div className={'mt-6 flex items-center'}>
                                            <div className={'flex-1'}>
                                                <div className={'border-l-4 border-cyan-500 p-3 bg-cyan-950/10'}>
                                                    <p className={'text-xs text-neutral-200'}>
                                                        Your SFTP password is the same as the password you use to access this panel.
                                                    </p>
                                                </div>
                                            </div>
                                            <div className={'ml-4'}>
                                                <a href={`sftp://${username}.template.${template.id.split('-')[0]}@${window.location.hostname}:2022`}>
                                                    <Button.Text variant={Button.Variants.Secondary}>Launch SFTP</Button.Text>
                                                </a>
                                            </div>
                                        </div>
                                    </TitledGreyBox>
                                </div>
                                <div className={'w-full mt-6 md:flex-1 md:mt-0'}>
                                    <TitledGreyBox title={'Template Directory'}>
                                        <div>
                                            <Label>Golden Template Path</Label>
                                            <CopyOnClick text={`/var/lib/pterodactyl/templates/${template.id}`}>
                                                <Input type={'text'} value={`/var/lib/pterodactyl/templates/${template.id}`} readOnly />
                                            </CopyOnClick>
                                        </div>
                                        <div className={'mt-6'}>
                                            <Label>Template ID</Label>
                                            <CopyOnClick text={template.id}>
                                                <Input type={'text'} value={template.id} readOnly />
                                            </CopyOnClick>
                                        </div>
                                    </TitledGreyBox>
                                </div>
                            </div>
                        </Route>

                        <Route path={'/stratus/templates/:id/settings'} exact>
                            <form onSubmit={(e) => e.preventDefault()} className={'bg-neutral-900 p-6 rounded shadow-lg border border-neutral-700'}>
                                <div className={'flex justify-between items-center mb-6 border-b border-neutral-800 pb-4'}>
                                    <div>
                                        <h3 className={'text-xl font-header text-neutral-200'}>Edit Template Settings</h3>
                                        <p className={'text-neutral-400 text-xs mt-1'}>
                                            Configure core resource constraints and environments applied to dynamic instances.
                                        </p>
                                    </div>
                                    <div className={'flex items-center space-x-2 text-xs font-mono'}>
                                        {autosaveState === 'saving' && (
                                            <span className={'text-cyan-400 flex items-center space-x-1'}>
                                                <Spinner size={'small'} />
                                                <span>Saving...</span>
                                            </span>
                                        )}
                                        {autosaveState === 'saved' && (
                                            <span className={'text-emerald-400 flex items-center space-x-1.5'}>
                                                <FontAwesomeIcon icon={faSave} />
                                                <span>All changes saved!</span>
                                            </span>
                                        )}
                                        {autosaveState === 'error' && (
                                            <span className={'text-red-400 flex items-center space-x-1'}>
                                                <span>⚠️ Save failed</span>
                                            </span>
                                        )}
                                        {autosaveState === 'idle' && (
                                            <span className={'text-neutral-500'}>
                                                <span>Saved to cloud</span>
                                            </span>
                                        )}
                                    </div>
                                </div>
                                
                                <FlashMessageRender byKey={'stratus:template-settings'} className={'mb-4'} />

                                <div className={'grid grid-cols-1 md:grid-cols-2 gap-6'}>
                                    <div>
                                        <Label>Template Name</Label>
                                        <Input
                                            type={'text'}
                                            value={name}
                                            onChange={(e) => setName(e.target.value)}
                                            onBlur={() => autosaveSettings({ name })}
                                            required
                                        />
                                    </div>
                                    <div>
                                         <Label>Pterodactyl Egg</Label>
                                         <Select
                                             value={eggId}
                                             onChange={(e) => {
                                                 const val = parseInt(e.target.value) || 1;
                                                 setEggId(val);
                                                 const newEgg = eggs?.find(eg => eg.id === val);
                                                 let newImg = image;
                                                 const eggImages = getEggImages(newEgg);
                                                 if (eggImages.length > 0) {
                                                     newImg = eggImages[0].image;
                                                     setImage(newImg);
                                                 }
                                                 autosaveSettings({ eggId: val, image: newImg });
                                             }}
                                             required
                                         >
                                             <option value="" disabled className={'bg-neutral-800 text-neutral-200'}>Select an Egg</option>
                                             {eggs?.map((egg) => (
                                                 <option key={egg.id} value={egg.id} className={'bg-neutral-800 text-neutral-200'}>{egg.name} (ID: {egg.id})</option>
                                             ))}
                                         </Select>
                                     </div>

                                    <div>
                                        <Label>Memory Limit (MB)</Label>
                                        <Input
                                            type={'number'}
                                            value={memory}
                                            onChange={(e) => setMemory(parseInt(e.target.value) || 2048)}
                                            onBlur={() => autosaveSettings({ memory })}
                                            required
                                        />
                                    </div>
                                    <div>
                                        <Label>Disk Limit (MB)</Label>
                                        <Input
                                            type={'number'}
                                            value={disk}
                                            onChange={(e) => setDisk(parseInt(e.target.value) || 5120)}
                                            onBlur={() => autosaveSettings({ disk })}
                                            required
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <Label>CPU Limit (%) (0 for Unlimited)</Label>
                                        <Input
                                            type={'number'}
                                            value={cpu}
                                            onChange={(e) => setCpu(parseInt(e.target.value) || 0)}
                                            onBlur={() => autosaveSettings({ cpu })}
                                            required
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <Label>Startup Command</Label>
                                        <Input
                                            type={'text'}
                                            value={startup}
                                            onChange={(e) => setStartup(e.target.value)}
                                            onBlur={() => autosaveSettings({ startup })}
                                            required
                                        />
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <div className={'flex justify-between items-center mb-2'}>
                                            <Label className={'!mb-0'}>Docker Image</Label>
                                            <Label className={'flex items-center cursor-pointer hover:text-neutral-300 !mb-0'}>
                                                <input 
                                                    type={'checkbox'} 
                                                    checked={useCustomImage} 
                                                    onChange={(e) => {
                                                        const checked = e.target.checked;
                                                        setUseCustomImage(checked);
                                                        if (!checked) {
                                                            const selectedEgg = eggs?.find(eg => eg.id === eggId);
                                                            const eggImages = getEggImages(selectedEgg);
                                                            if (eggImages.length > 0) {
                                                                const firstImg = eggImages[0].image;
                                                                setImage(firstImg);
                                                                autosaveSettings({ image: firstImg });
                                                            }
                                                        }
                                                    }}
                                                    className={'rounded bg-neutral-800 border-neutral-700 text-cyan-600 focus:ring-cyan-500 mr-1.5'}
                                                />
                                                <span>Use custom image</span>
                                            </Label>
                                        </div>

                                        {useCustomImage || getEggImages(eggs?.find(eg => eg.id === eggId)).length === 0 ? (
                                            <Input
                                                type={'text'}
                                                value={image}
                                                onChange={(e) => setImage(e.target.value)}
                                                onBlur={() => autosaveSettings({ image })}
                                                required
                                                placeholder={'e.g. ghcr.io/pterodactyl/yolks:java_17'}
                                            />
                                        ) : (
                                            <Select
                                                value={image}
                                                onChange={(e) => {
                                                    setImage(e.target.value);
                                                    autosaveSettings({ image: e.target.value });
                                                }}
                                                required
                                            >
                                                {getEggImages(eggs?.find(eg => eg.id === eggId)).map(({ image: img, label }) => (
                                                    <option key={img} value={img} className={'bg-neutral-800 text-neutral-200'}>{label} ({img})</option>
                                                ))}
                                            </Select>
                                        )}
                                    </div>

                                    <div className={'md:col-span-2'}>
                                        <div className={'flex justify-between items-center mb-3'}>
                                            <Label className={'!mb-0'}>Environment Variables</Label>
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
                                                    <Input
                                                        type={'text'}
                                                        value={row.key}
                                                        onChange={(e) => handleEnvRowChange(idx, 'key', e.target.value)}
                                                        onBlur={() => autosaveSettings()}
                                                        placeholder={'KEY'}
                                                        className={'!p-2.5 font-mono text-xs'}
                                                    />
                                                    <Input
                                                        type={'text'}
                                                        value={row.value}
                                                        onChange={(e) => handleEnvRowChange(idx, 'value', e.target.value)}
                                                        onBlur={() => autosaveSettings()}
                                                        placeholder={'value'}
                                                        className={'!p-2.5 font-mono text-xs'}
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
                                    <span className={'text-neutral-500 text-xs font-mono border border-neutral-850 px-4 py-2 rounded bg-neutral-950/20 flex items-center space-x-1.5'}>
                                        <span className={'w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse'}></span>
                                        <span>Settings Autosaved</span>
                                    </span>
                                </div>
                            </form>
                        </Route>
                    </Switch>
                </div>
            </div>
        </PageContentBlock>
    );
};
