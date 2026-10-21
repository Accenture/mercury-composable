// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest';
import { saveBinaryFile, saveImageFile, saveTextFile, supportsSaveFilePicker } from '../graphFile';

type PickerWindow = Window & { showSaveFilePicker?: unknown };

afterEach(() => {
  delete (window as PickerWindow).showSaveFilePicker;
  vi.restoreAllMocks();
});

describe('saveBinaryFile', () => {
  it('writes the bytes through the native dialog as a .pack and answers the chosen name', async () => {
    const write = vi.fn().mockResolvedValue(undefined);
    const close = vi.fn().mockResolvedValue(undefined);
    const picker = vi.fn().mockResolvedValue({ name: 'renamed.pack', createWritable: async () => ({ write, close }) });
    (window as PickerWindow).showSaveFilePicker = picker;
    expect(supportsSaveFilePicker()).toBe(true);

    const bytes = new Uint8Array([0x82, 0xa8, 0x6d, 0x61]);
    const outcome = await saveBinaryFile(bytes, 'demo.pack');

    expect(outcome).toEqual({ saved: true, fileName: 'renamed.pack' });
    expect(picker).toHaveBeenCalledWith({
      suggestedName: 'demo.pack',
      excludeAcceptAllOption: true,
      types: [{ description: 'MiniGraph graph set', accept: { 'application/octet-stream': ['.pack'] } }],
    });
    expect(write).toHaveBeenCalledWith(bytes);
    expect(close).toHaveBeenCalledTimes(1);
  });

  it('reports a cancelled dialog without throwing', async () => {
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockRejectedValue(Object.assign(new Error('cancelled'), { name: 'AbortError' }));
    expect(await saveBinaryFile(new Uint8Array([1]), 'demo.pack')).toEqual({ saved: false, cancelled: true });
  });

  it('falls back to a download of an octet-stream blob under the file name', async () => {
    expect(supportsSaveFilePicker()).toBe(false);
    const blobs: Blob[] = [];
    vi.spyOn(URL, 'createObjectURL').mockImplementation((blob: Blob | MediaSource) => {
      blobs.push(blob as Blob);
      return 'blob:demo';
    });
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clicked: HTMLAnchorElement[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicked.push(this);
    });

    const outcome = await saveBinaryFile(new Uint8Array([1, 2, 3]), 'demo.pack');

    expect(outcome).toEqual({ saved: true, fileName: 'demo.pack' });
    expect(blobs).toHaveLength(1);
    expect(blobs[0].type).toBe('application/octet-stream');
    expect(blobs[0].size).toBe(3);
    expect(clicked).toHaveLength(1);
    expect(clicked[0].download).toBe('demo.pack');
    expect(clicked[0].href).toContain('blob:demo');
  });
});

describe('saveTextFile', () => {
  it('still offers the JSON type through the native dialog', async () => {
    const write = vi.fn().mockResolvedValue(undefined);
    const picker = vi.fn().mockResolvedValue({ name: 'demo.json', createWritable: async () => ({ write, close: vi.fn() }) });
    (window as PickerWindow).showSaveFilePicker = picker;

    await saveTextFile('{}', 'demo.json');

    expect(picker.mock.calls[0][0].types).toEqual([{ description: 'MiniGraph model (JSON)', accept: { 'application/json': ['.json'] } }]);
    expect(write).toHaveBeenCalledWith('{}');
  });
});

describe('saveImageFile', () => {
  /** A handle the native dialog answers, with the file it points at (Chromium creates it empty). */
  function pickedFile(existing: string = '') {
    const file = { content: existing, removed: false };
    const write = vi.fn().mockImplementation(async (data: Blob) => { file.content = await data.text(); });
    const close = vi.fn().mockResolvedValue(undefined);
    const abort = vi.fn().mockResolvedValue(undefined);
    const handle = {
      name: 'chosen.png',
      createWritable: vi.fn().mockResolvedValue({ write, close, abort }),
      getFile: vi.fn().mockImplementation(async () => new File([file.content], 'chosen.png')),
      remove: vi.fn().mockImplementation(async () => { file.removed = true; }),
    };
    return { file, handle, write, close, abort };
  }

  it('opens the native dialog before rendering, then writes the image', async () => {
    const order: string[] = [];
    const png = new Blob(['png'], { type: 'image/png' });
    const { handle, write } = pickedFile();
    const picker = vi.fn().mockImplementation(async () => {
      order.push('dialog');
      return handle;
    });
    (window as PickerWindow).showSaveFilePicker = picker;
    const render = vi.fn().mockImplementation(async () => {
      order.push('render');
      return png;
    });

    const outcome = await saveImageFile(render, 'demo.png');

    expect(order).toEqual(['dialog', 'render']);
    expect(outcome).toEqual({ saved: true, fileName: 'chosen.png' });
    expect(picker.mock.calls[0][0]).toEqual({
      suggestedName: 'demo.png',
      excludeAcceptAllOption: true,
      types: [{ description: 'PNG image', accept: { 'image/png': ['.png'] } }],
    });
    expect(write).toHaveBeenCalledWith(png);
    expect(handle.remove).not.toHaveBeenCalled();
  });

  it('opens the dialog in the same task as the call, so the click still counts', () => {
    const picker = vi.fn().mockReturnValue(new Promise(() => undefined));
    (window as PickerWindow).showSaveFilePicker = picker;

    void saveImageFile(vi.fn(), 'demo.png');

    expect(picker).toHaveBeenCalledTimes(1);
  });

  it('renders nothing when the dialog is cancelled', async () => {
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockRejectedValue(Object.assign(new Error('cancelled'), { name: 'AbortError' }));
    const render = vi.fn();

    expect(await saveImageFile(render, 'demo.png')).toEqual({ saved: false, cancelled: true });
    expect(render).not.toHaveBeenCalled();
  });

  it('passes a render failure on and removes the empty file the dialog left', async () => {
    const { file, handle } = pickedFile();
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockResolvedValue(handle);

    await expect(saveImageFile(() => Promise.reject(new Error('no canvas')), 'demo.png')).rejects.toThrow('no canvas');
    expect(file.removed).toBe(true);
    expect(handle.createWritable).not.toHaveBeenCalled();
  });

  it('keeps a chosen file that still holds its old content', async () => {
    const { file, handle } = pickedFile('an older image');
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockResolvedValue(handle);

    await expect(saveImageFile(() => Promise.reject(new Error('no canvas')), 'demo.png')).rejects.toThrow('no canvas');
    expect(file.removed).toBe(false);
  });

  it('aborts a failed write and removes the empty file', async () => {
    const { file, handle, write, close, abort } = pickedFile();
    write.mockRejectedValue(new Error('disk full'));
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockResolvedValue(handle);

    await expect(saveImageFile(async () => new Blob(['png']), 'demo.png')).rejects.toThrow('disk full');
    expect(abort).toHaveBeenCalledTimes(1);
    expect(close).not.toHaveBeenCalled();
    expect(file.removed).toBe(true);
  });

  it('reports the save failure, not the cleanup, where the browser cannot remove the file', async () => {
    const { handle } = pickedFile();
    const withoutRemove = { name: handle.name, createWritable: handle.createWritable, getFile: handle.getFile };
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockResolvedValue(withoutRemove);
    await expect(saveImageFile(() => Promise.reject(new Error('no canvas')), 'demo.png')).rejects.toThrow('no canvas');

    handle.getFile.mockRejectedValue(new Error('NotAllowedError'));
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockResolvedValue(handle);
    await expect(saveImageFile(() => Promise.reject(new Error('no canvas')), 'demo.png')).rejects.toThrow('no canvas');
    expect(handle.remove).not.toHaveBeenCalled();
  });

  it('without the dialog, renders and downloads the image under the file name', async () => {
    const blobs: Blob[] = [];
    vi.spyOn(URL, 'createObjectURL').mockImplementation((blob: Blob | MediaSource) => {
      blobs.push(blob as Blob);
      return 'blob:image';
    });
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clicked: HTMLAnchorElement[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicked.push(this);
    });

    const outcome = await saveImageFile(async () => new Blob(['png'], { type: 'image/png' }), 'demo.png');

    expect(outcome).toEqual({ saved: true, fileName: 'demo.png' });
    expect(blobs[0].type).toBe('image/png');
    expect(await blobs[0].text()).toBe('png');
    expect(clicked[0].download).toBe('demo.png');
  });
});
