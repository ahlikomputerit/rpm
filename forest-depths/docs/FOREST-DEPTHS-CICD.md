# Forest Depths — CI/CD Deployment Guide

## Scope

Forest Depths now includes GitHub Actions workflows for quality gates and optional deployment to Vercel or Netlify. The workflows are designed for the repository layout where the application lives under `forest-depths/` and the source branch is `forest-depths`.

Manus built-in hosting remains the simplest managed option for this project. Vercel and Netlify are external hosting alternatives requested for portability. External deployment may require separate domain configuration, asset lifecycle review, and provider-specific secrets.

## Workflow map

| Workflow | Trigger | Behavior |
|---|---|---|
| `.github/workflows/forest-ci.yml` | Pull request, push to `forest-depths`, manual | Installs with frozen lockfile, runs TypeScript check, runs production build |
| `.github/workflows/forest-vercel.yml` | Pull request, push to `forest-depths`, manual | Quality gates, Vercel prebuilt preview for PR, production deploy for branch push |
| `.github/workflows/forest-netlify.yml` | Pull request, push to `forest-depths`, manual | Quality gates, Netlify draft deploy for PR, production deploy for branch push |

Only enable one production provider as the canonical public deployment unless there is a deliberate reason to maintain two public copies. Both workflow files are present so the repository can support either provider without rewriting the pipeline.

## Required GitHub secrets

### Vercel

Create a Vercel project for the `forest-depths` application and add these repository or environment secrets:

| Secret | Purpose |
|---|---|
| `VERCEL_TOKEN` | Vercel API token used by the CLI |
| `VERCEL_ORG_ID` | Organization/team identifier from Vercel project settings |
| `VERCEL_PROJECT_ID` | Vercel project identifier |

The workflow uses the documented Vercel CLI pattern of pulling project settings, building a prebuilt artifact, and deploying that artifact [1] [2]. The token must be stored only in GitHub Actions Secrets and must never be committed to `vercel.json` or source code.

### Netlify

Create a Netlify site for the `forest-depths` application and add:

| Secret | Purpose |
|---|---|
| `NETLIFY_AUTH_TOKEN` | Netlify personal access token or deploy token |
| `NETLIFY_SITE_ID` | Netlify site identifier |

The workflow uses Netlify CLI deployment to the explicit `dist/public` directory. Netlify documents CLI authentication and deployment through access credentials [3]. Never place the token in `netlify.toml` or a shell script committed to the repository.

## First-time setup

1. Confirm the `forest-depths` branch contains the application and workflow files.
2. Create exactly one provider project initially, either Vercel or Netlify.
3. Set the provider secrets in GitHub repository settings. Prefer an environment named `production` for production-only secrets when the repository requires approval gates.
4. Confirm that `dist/public` is the intended static output. The project build also emits a server bundle for the original managed template, but the external static deployments publish only the Vite client output.
5. Open a pull request that changes a Forest file. Confirm the CI job passes before enabling production deployment.
6. Confirm the PR preview URL and test initial load, chapter navigation, image asset requests, hotspot observation, audio toggle, and reduced-motion fallback.
7. Merge or push to `forest-depths` only after the preview passes. The provider workflow then performs production deployment.

## Provider-specific notes

### Vercel

The repository includes `vercel.json` with Vite framework metadata, frozen-lockfile installation, `pnpm run build`, and `dist/public` output. Vercel's GitHub Actions guidance uses `vercel pull`, `vercel build`, and `vercel deploy --prebuilt` to separate artifact creation from deployment [1]. Vercel's Vite documentation also supports Git-based preview URLs for pull requests [2].

### Netlify

The repository includes `netlify.toml` with `pnpm run build`, `dist/public`, an SPA fallback to `/index.html`, basic security headers, and immutable caching for generated `/assets/` files. The workflow performs a draft deploy for pull requests and a production deploy for pushes to `forest-depths`.

## Branch and trigger policy

The workflow is intentionally scoped to changes under `forest-depths/**` and workflow files. Changes to unrelated directories in the monorepo do not trigger Forest deployment. Pull requests receive quality gates and preview deployments. Only pushes to `forest-depths` trigger production deployment in the supplied workflows.

If the branch is renamed, update the `branches` and `paths` filters in all three workflow files. If the application is moved from `forest-depths/`, update workflow working directories, lockfile paths, provider output configuration, and this guide together.

## Rollback

Rollback should be performed through the provider's deployment history first. Select the last known-good deployment whose commit matches a passing Forest QA report. Do not rewrite history or force-push the production branch as a rollback mechanism. After rollback, open a corrective pull request and let CI generate a new preview before restoring the branch.

## Asset and security caveats

Forest visual assets currently use lifecycle-safe storage URLs generated by the Manus asset workflow. Confirm those URLs remain publicly readable from the external provider and do not depend on a sandbox-only hostname. If an asset host requires authentication, external static deployment will render missing textures even when the HTML build succeeds.

The workflows do not expose API keys to the browser. Audio is procedural and does not require a provider secret. Do not add Manus connector tokens, Vercel tokens, Netlify tokens, or private asset credentials to `VITE_*` variables unless they are explicitly intended to be public.

## Local validation

Run these commands from `forest-depths/` before pushing:

```bash
pnpm install --frozen-lockfile
pnpm run check
pnpm run build
```

The external workflows use the same commands. A successful local build does not prove provider deployment success; confirm output directory, asset requests, SPA fallback, and browser runtime on the provider URL.

## References

[1]: https://vercel.com/kb/guide/how-can-i-use-github-actions-with-vercel "Vercel — How can I use GitHub Actions with Vercel?"
[2]: https://vercel.com/docs/frameworks/frontend/vite "Vercel — Vite"
[3]: https://docs.netlify.com/api-and-cli-guides/cli-guides/get-started-with-cli/ "Netlify — Get started with the CLI"
[4]: https://vite.dev/guide/static-deploy "Vite — Static deployment"
