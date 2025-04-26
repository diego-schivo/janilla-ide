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
import { WebComponent } from "./web-component.js";

export default class RootLayout extends WebComponent {

	static get templateName() {
		return "root-layout";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		const o = JSON.parse(document.querySelector("#state").text);
		Object.assign(this.state, o);
		this.addEventListener("select-entry", this.handleSelectEntry);
		this.addEventListener("expand-directory", this.handleExpandDirectory);
		this.addEventListener("collapse-directory", this.handleCollapseDirectory);
		this.addEventListener("close-entry", this.handleCloseEntry);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("select-entry", this.handleSelectEntry);
		this.removeEventListener("expand-directory", this.handleExpandDirectory);
		this.removeEventListener("collapse-directory", this.handleCollapseDirectory);
		this.removeEventListener("close-entry", this.handleCloseEntry);
	}

	handleCloseEntry = event => {
		const p = event.detail.path;
		const s = this.state;
		const i = s.paths.indexOf(p);
		s.paths.splice(i, 1);
		s.activeIndex = Math.min(s.activeIndex, s.paths.length - 1);
		history.pushState(null, "", s.activeIndex !== -1 ? `/entry/${s.paths[s.activeIndex]}` : "/");
		this.querySelector("entry-tree").requestDisplay();
		this.querySelector("entry-list").requestDisplay();
	}

	handleCollapseDirectory = event => {
		const p = event.detail.path;
		const d = p.split("/").reduce((x, y) => x.entries.find(z => z.name === y), this.state);
		d.entries = null;
		this.querySelector("entry-tree").requestDisplay();
	}

	handleExpandDirectory = async event => {
		const p = event.detail.path;
		const d = (p ? p.split("/") : []).reduce((x, y) => x.entries.find(z => z.name === y), this.state);
		const j = await (await fetch(`/api/entries/${p ?? ""}`)).json();
		d.entries = j.entries;
		this.querySelector("entry-tree").requestDisplay();
	}

	handleSelectEntry = event => {
		const p = event.detail.path;
		const s = this.state;
		const i = s.paths.indexOf(p);
		if (i === -1) {
			s.activeIndex = s.paths.length;
			s.paths.push(p);
		} else if (i !== s.activeIndex)
			s.activeIndex = i;
		else
			return;
		history.pushState(null, "", p ? `/entry/${p}` : "/");
		this.querySelector("entry-tree").requestDisplay();
		this.querySelector("entry-list").requestDisplay();
	}
}
