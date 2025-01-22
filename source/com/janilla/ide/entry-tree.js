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
import { FlexibleElement } from "./flexible-element.js";

export default class EntryTree extends FlexibleElement {

	static get observedAttributes() {
		return ["data-path"];
	}

	static get templateName() {
		return "entry-tree";
	}

	get state() {
		return this.closest("janilla-ide").state.entryTree;
	}

	set state(x) {
		const ji = this.closest("janilla-ide");
		if (x != null && !ji.state)
			ji.state = {};
		if (x != null || ji.state)
			ji.state.entryTree = x;
	}

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("EntryTree.connectedCallback");
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		// console.log("EntryTree.disconnectedCallback");
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		// console.log("EntryTree.handleClick", event);
		if (event.target.tagName.toLowerCase() === "li") {
			event.stopPropagation();
			const en = event.target.querySelector("entry-node");
			const p = en.dataset.path;
			let s = this.state;
			let nn = s.nodes;
			for (const x of p.split("/")) {
				s = nn[x];
				nn = s.children;
			}
			if (en.dataset.expanded == null) {
				const j = await (await fetch(`/api/entries/${p}`)).json();
				s.children = Object.fromEntries(j.map(x => ([x.name, {
					path: [p, x.name].join("/"),
					...x
				}])));
			} else
				delete s.children;
			history.pushState(this.closest("janilla-ide").state, "", "/");
			en.parentElement.closest("entry-tree, entry-node").requestUpdate();
		/*
		} else {
			const en = event.target.closest("entry-node");
			const p = en.dataset.path;
			let s = this.state;
			p.split("/").forEach((x, i, xx) => {
				if (i < xx.length - 1)
					s = s.children[x];
				else
					Object.entries(s.children).forEach(([k, v]) => v.active = k === x);
			});
			en.parentElement.closest("entry-tree, entry-node").requestUpdate();
		*/
		}
	}

	async updateDisplay() {
		// console.log("EntryTree.updateDisplay");
		if (!this.state) {
			const j = await (await fetch("/api/entries/")).json();
			this.state = {
				nodes: Object.fromEntries(j.map(x => ([x.name, {
					path: x.name,
					expandable: x.expandable
				}])))
			};
			history.pushState(this.closest("janilla-ide").state, "", "/");
		}
		const p = this.closest("janilla-ide").state.path;
		this.appendChild(this.interpolateDom({
			$template: "",
			items: Object.values(this.state.nodes).map(x => ({
				$template: "item",
				...x,
				expanded: !!x.children,
				active: x.name === p
			}))
		}));
	}
}
