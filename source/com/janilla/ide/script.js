/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import EntryItem from "./entry-item.js";
import EntryList from "./entry-list.js";
import EntryManager from "./entry-manager.js";
import EntryNode from "./entry-node.js";
import EntryTree from "./entry-tree.js";
import FileEditor from "./file-editor.js";
import JanillaIde from "./janilla-ide.js";

customElements.define("entry-item", EntryItem);
customElements.define("entry-list", EntryList);
customElements.define("entry-manager", EntryManager);
customElements.define("entry-node", EntryNode);
customElements.define("entry-tree", EntryTree);
customElements.define("file-editor", FileEditor);
customElements.define("janilla-ide", JanillaIde);

const initState = () => {
	const el = document.getElementById("state");
	const s = el ? JSON.parse(el.text) : {};
	history.replaceState(s, "");
	dispatchEvent(new CustomEvent("popstate"));
}

if (document.readyState === "loading")
	document.addEventListener("DOMContentLoaded", initState);
else
	initState();
