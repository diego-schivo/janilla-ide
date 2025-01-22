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

export default class EntryList extends FlexibleElement {

	static get observedAttributes() {
		return ["data-path"];
	}

	static get templateName() {
		return "entry-list";
	}

	get state() {
		return this.closest("janilla-ide").state.entryList;
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	async updateDisplay() {
		// console.log("EntryList.updateDisplay");
		const s = this.state;
		const p = this.closest("janilla-ide").state.path;
		this.shadowRoot.appendChild(this.interpolateDom({
			$template: "shadow",
			items: s.items.map(x => ({
				$template: "shadow-item",
				...x,
				class: x.path === p ? "active" : null,
				name: x.path.split("/").at(-1)
			}))
		}));
		this.appendChild(this.interpolateDom({
			$template: "",
			items: s.items.map((x, i) => ({
				$template: "item",
				...x,
				slot: x.path === p ? "content" : null
			}))
		}));
	}
}
