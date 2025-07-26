# GitHub Actions - Home Assistant Sync Setup

This document explains how the GitHub Action automatically synchronizes Home Assistant scripts within the monorepo.

## 🔧 How It Works

The workflow automatically syncs content from `home-assistant-scripts/` to `home-assistant-config/custom/` within the same repository.

## 🏗️ Directory Structure

```
home-assistant-log-api/ (monorepo)
├── logs-api/                    # ← Spring Boot API (deploys to AWS)
├── home-assistant-scripts/      # ← Source scripts (development)
├── home-assistant-config/       # ← Synced config (Home Assistant uses)
│   └── custom/                  # ← Auto-generated from scripts
├── terraform/                  # ← AWS Infrastructure
└── docs/                       # ← Documentation
```

## ✅ No External Configuration Required

Since this is **internal synchronization** within the same repository:
- ❌ **No secrets needed**
- ❌ **No external tokens required**  
- ❌ **No additional repositories**
- ✅ **Works automatically** with repository permissions

## 🎯 Workflow Behavior

### **Automatic Trigger**
The workflow runs automatically when:
- ✅ Changes are pushed to `main` branch
- ✅ Changes affect files in `home-assistant-scripts/` directory

### **Manual Trigger**
You can also trigger manually:
1. Go to **Actions** tab in your repository
2. Select **"Sync Home Assistant Scripts"** workflow
3. Click **"Run workflow"** → **"Run workflow"**

### **What It Does**
1. **Detects changes** in `home-assistant-scripts/` directory
2. **Clones** the target Home Assistant repository
3. **Replaces** the `custom/` directory with contents from `home-assistant-scripts/`
4. **Commits and pushes** changes to target repository
5. **Creates summary** of the sync operation

## 📁 Directory Mapping

```
Source                          Target (Auto-synced)
home-assistant-scripts/    →    home-assistant-config/custom/
├── statistics/           →    ├── statistics/
├── templates/            →    ├── templates/
├── automations/          →    ├── automations/
├── scripts/              →    ├── scripts/
└── integrations/         →    └── integrations/
```

## 🏠 Home Assistant Configuration

Point your Home Assistant to use the synced configuration directory. In your Home Assistant `configuration.yaml`:

```yaml
# Modular Custom Configurations (point to synced directory)
sensor: !include_dir_merge_list /path/to/home-assistant-config/custom/statistics/
template: !include_dir_merge_list /path/to/home-assistant-config/custom/templates/
automation manual: !include_dir_merge_list /path/to/home-assistant-config/custom/automations/
script: !include_dir_merge_named /path/to/home-assistant-config/custom/scripts/

# Add integration includes as needed
# rest_command: !include /path/to/home-assistant-config/custom/integrations/api-integrations/home-assistant-logs.api.yaml
```

**Note**: Replace `/path/to/home-assistant-config/` with the actual path where you clone this repository on your Home Assistant server.

## 🔍 Monitoring & Troubleshooting

### **Check Workflow Status**
- **Actions Tab**: Monitor workflow runs and view logs
- **Commit History**: Verify commits were made to target repository
- **Summary**: Check workflow summary for sync details

### **Common Issues**

| Issue | Cause | Solution |
|-------|-------|----------|
| `Authentication failed` | Invalid or expired token | Generate new Personal Access Token |
| `Repository not found` | Wrong repository name | Check `HOME_ASSISTANT_REPO` secret format |
| `Permission denied` | Token lacks permissions | Ensure token has `repo` scope |
| `No changes to sync` | No actual changes detected | Normal behavior, workflow exits gracefully |

### **Debugging Steps**
1. **Check secrets**: Verify both secrets are correctly set
2. **Review logs**: Check workflow logs in Actions tab
3. **Test manually**: Use `workflow_dispatch` to test manually
4. **Verify target**: Ensure target repository exists and is accessible

## 🚀 Testing the Setup

1. **Configure secrets** as described above
2. **Make a test change** to any file in `home-assistant-scripts/`
3. **Commit and push** to `main` branch
4. **Monitor** the Actions tab for workflow execution
5. **Verify** changes appear in your Home Assistant repository

## 📝 Best Practices

- ✅ **Test with non-critical changes** first
- ✅ **Monitor workflows** for the first few syncs
- ✅ **Keep tokens secure** and rotate periodically
- ✅ **Backup** your Home Assistant configuration before first sync
- ❌ **Don't commit secrets** to your repository

---

**🔄 Once configured, changes to `home-assistant-scripts/` will automatically sync to your Home Assistant repository on every merge to main!** 