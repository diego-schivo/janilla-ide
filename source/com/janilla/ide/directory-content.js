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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class DirectoryContent extends UpdatableHTMLElement {

	static get templateName() {
		return "directory-content";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleSubmit = async event => {
		event.preventDefault();
		const fd = new FormData(event.target);
		const o = [...fd.entries()].reduce((x, y) => {
			switch (y[0]) {
				case "names":
					(x.names ??= []).push(y[1]);
					break;
				default:
					x[y[0]] = y[1];
					break;
			}
			return x;
		}, { $type: "Directory" });
		if (event.submitter.value)
			o[event.submitter.name] = event.submitter.value;
		const p = this.closest("editor-list").dataset.path ?? null;
		await (await fetch(`/api/entries/${p ?? ""}`, {
			method: event.submitter.name === "create" ? "POST" : "PUT",
			headers: { "content-type": "application/json" },
			body: JSON.stringify(o)
		})).json();
	}

	async updateDisplay() {
		const s = this.closest("editor-list").state;
		this.appendChild(this.interpolateDom({
			$template: "",
			entryItems: s.entry.entries.map(x => ({
				$template: "entry-item",
				...x
			})),
			importForm: !s.path ? { $template: "import-form" } : null,
			branchForm: s.entry.repository ? {
				$template: "branch-form",
				...s.entry.repository,
				options: s.entry.repository.branches.map(x => ({
					$template: "branch-option",
					value: x
				}))
			} : null
		}));
	}
}
