/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.addon.marketplace.internal.community;

import static org.openhab.core.addon.Addon.CODE_MATURITY_LEVELS;
import static org.openhab.core.addon.marketplace.MarketplaceConstants.*;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.marketplace.AbstractRemoteAddonService;
import org.openhab.core.addon.marketplace.AddonVersion;
import org.openhab.core.addon.marketplace.VersionedAddon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.Version;
import org.openhab.core.addon.marketplace.VersionRange;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscoursePosterInfo;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscourseTopicItem;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseCategoryResponseDTO.DiscourseUser;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponseDTO;
import org.openhab.core.addon.marketplace.internal.community.model.DiscourseTopicResponseDTO.DiscoursePostLink;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.StorageService;
import org.openhab.core.util.UIDUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an {@link org.openhab.core.addon.AddonService} retrieving posts on community.openhab.org (Discourse).
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(immediate = true, configurationPid = CommunityMarketplaceAddonService.SERVICE_PID, //
        property = Constants.SERVICE_PID + "="
                + CommunityMarketplaceAddonService.SERVICE_PID, service = AddonService.class)
@ConfigurableService(category = "system", label = CommunityMarketplaceAddonService.SERVICE_NAME, description_uri = CommunityMarketplaceAddonService.CONFIG_URI)
@NonNullByDefault
public class CommunityMarketplaceAddonService extends AbstractRemoteAddonService {
    public static final String CODE_CONTENT_SUFFIX = "_content";
    public static final String JSON_CONTENT_PROPERTY = "json" + CODE_CONTENT_SUFFIX;
    public static final String YAML_CONTENT_PROPERTY = "yaml" + CODE_CONTENT_SUFFIX;

    // constants for the configuration properties
    static final String SERVICE_NAME = "Community Marketplace";
    static final String SERVICE_PID = "org.openhab.marketplace";
    static final String CONFIG_URI = "system:marketplace";
    static final String CONFIG_API_KEY = "apiKey";
    static final String CONFIG_SHOW_UNPUBLISHED_ENTRIES_KEY = "showUnpublished";
    static final String CONFIG_ENABLED_KEY = "enable";

    private static final String COMMUNITY_BASE_URL = "https://community.openhab.org";
    private static final String COMMUNITY_MARKETPLACE_URL = COMMUNITY_BASE_URL + "/c/marketplace/69/l/latest";
    private static final String COMMUNITY_TOPIC_URL = COMMUNITY_BASE_URL + "/t/";
    private static final Pattern BUNDLE_NAME_PATTERN = Pattern.compile(".*/(.*?)-\\d+\\.\\d+\\.\\d+.*");

    private static final String SERVICE_ID = "marketplace";
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";

    private static final Pattern CODE_MARKUP_PATTERN = Pattern.compile(
            "<pre(?: data-code-wrap=\"[-a-zA-Z]+\")?><code class=\"lang-(?<lang>[-a-zA-Z]+)\">(?<content>.*?)</code></pre>\\n?",
            Pattern.DOTALL);
    private static final Pattern CODE_INLINE_RESOURCE_PATTERN = Pattern.compile(
        "<pre(?: data-code-wrap=\"(?i)(?:yaml|json)(?-i)\")?><code class=\"lang-(?i)(?<lang>yaml|json)(?-i)\">(?<content>.*?)</code></pre>\\n?",
        Pattern.DOTALL);
    private static final Pattern CODE_ADDON_PATTERN = Pattern.compile(
            "<pre(?: data-code-wrap=\"(?i)(?:addon|add-on)(?-i)\")?><code class=\"lang-(?i)(?:addon|add-on)(?-i)\">(?<content>.*?)</code></pre>\\n?",
            Pattern.DOTALL);
    private static final Pattern CODE_VERSION_PATTERN = Pattern.compile(
        "<pre(?: data-code-wrap=\"(?i)version(?-i)\")?><code class=\"lang-(?i)version(?-i)\">(?<content>.*?)</code></pre>\\n?",
        Pattern.DOTALL);
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
            "^\\s*(?<key>\\w+)\\s*(?:=|:)\\s*(?<value>.*?)\\s*$", Pattern.MULTILINE);

    private static final Integer BUNDLES_CATEGORY = 73;
    private static final Integer RULETEMPLATES_CATEGORY = 74;
    private static final Integer UIWIDGETS_CATEGORY = 75;
    private static final Integer BLOCKLIBRARIES_CATEGORY = 76;
    private static final Integer TRANSFORMATIONS_CATEGORY = 80;

    private static final String PUBLISHED_TAG = "published";

    private static final Map<String, Set<String>> VALID_RESOURCE_TYPES = Map.of(
        JAR_CONTENT_TYPE, Set.of(JAR_DOWNLOAD_URL_PROPERTY),
        KAR_CONTENT_TYPE, Set.of(KAR_DOWNLOAD_URL_PROPERTY),
        RULETEMPLATES_CONTENT_TYPE, Set.of(JSON_DOWNLOAD_URL_PROPERTY, YAML_DOWNLOAD_URL_PROPERTY, JSON_CONTENT_PROPERTY, YAML_CONTENT_PROPERTY),
        TRANSFORMATIONS_CONTENT_TYPE, Set.of(JSON_DOWNLOAD_URL_PROPERTY, YAML_DOWNLOAD_URL_PROPERTY, JSON_CONTENT_PROPERTY, YAML_CONTENT_PROPERTY),
        UIWIDGETS_CONTENT_TYPE, Set.of(YAML_DOWNLOAD_URL_PROPERTY, YAML_CONTENT_PROPERTY),
        BLOCKLIBRARIES_CONTENT_TYPE, Set.of(YAML_DOWNLOAD_URL_PROPERTY, YAML_CONTENT_PROPERTY));

    private static final Set<String> RESOURCE_PROPERTY_NAMES = Set.of(JAR_DOWNLOAD_URL_PROPERTY, KAR_DOWNLOAD_URL_PROPERTY, JSON_DOWNLOAD_URL_PROPERTY, JSON_CONTENT_PROPERTY, YAML_DOWNLOAD_URL_PROPERTY, YAML_CONTENT_PROPERTY);

    private final Logger logger = LoggerFactory.getLogger(CommunityMarketplaceAddonService.class);

    private @Nullable String apiKey = null;
    private boolean showUnpublished = false;
    private boolean enabled = true;

    @Activate
    public CommunityMarketplaceAddonService(final @Reference EventPublisher eventPublisher,
            @Reference ConfigurationAdmin configurationAdmin, @Reference StorageService storageService,
            @Reference AddonInfoRegistry addonInfoRegistry, Map<String, Object> config) {
        super(eventPublisher, configurationAdmin, storageService, addonInfoRegistry, SERVICE_PID);
        modified(config);
    }

    @Modified
    public void modified(@Nullable Map<String, Object> config) {
        if (config != null) {
            this.apiKey = (String) config.get(CONFIG_API_KEY);
            this.showUnpublished = ConfigParser.valueAsOrElse(config.get(CONFIG_SHOW_UNPUBLISHED_ENTRIES_KEY),
                    Boolean.class, false);
            this.enabled = ConfigParser.valueAsOrElse(config.get(CONFIG_ENABLED_KEY), Boolean.class, true);
            cachedRemoteAddons.invalidateValue();
            refreshSource();
        }
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.add(handler);
    }

    @Override
    protected void removeAddonHandler(MarketplaceAddonHandler handler) {
        this.addonHandlers.remove(handler);
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    protected List<Addon> getRemoteAddons() {
        if (!enabled) {
            return List.of();
        }

        List<Addon> addons = new ArrayList<>();
        try {
            List<DiscourseCategoryResponseDTO> pages = new ArrayList<>();

            URL url = URI.create(COMMUNITY_MARKETPLACE_URL).toURL();
            int pageNb = 1;
            while (url != null) {
                URLConnection connection = url.openConnection();
                connection.addRequestProperty("Accept", "application/json");
                if (this.apiKey != null) {
                    connection.addRequestProperty("Api-Key", this.apiKey);
                }

                try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                    DiscourseCategoryResponseDTO parsed = gson.fromJson(reader, DiscourseCategoryResponseDTO.class);
                    if (parsed.topicList.topics.length != 0) {
                        pages.add(parsed);
                    }

                    if (parsed.topicList.moreTopicsUrl != null) {
                        // Discourse URL for next page is wrong
                        url = URI.create(COMMUNITY_MARKETPLACE_URL + "?page=" + pageNb++).toURL();
                    } else {
                        url = null;
                    }
                }
            }

            List<DiscourseUser> users = pages.stream().flatMap(p -> Stream.of(p.users)).toList();
            pages.stream().flatMap(p -> Stream.of(p.topicList.topics))
                    .filter(t -> showUnpublished || List.of(t.tags).contains(PUBLISHED_TAG))
                    .map(t -> Optional.ofNullable(convertTopicItemToAddon(t, users)))
                    .forEach(a -> a.ifPresent(addons::add));
        } catch (Exception e) {
            logger.warn("Unable to retrieve marketplace add-ons: {}", e.getMessage());
        }
        return addons;
    }

    @Override
    public @Nullable Addon getAddon(String uid, @Nullable Locale locale) {
        String queryId = uid.startsWith(ADDON_ID_PREFIX) ? uid : ADDON_ID_PREFIX + uid;

        // check if it is an installed add-on (cachedAddons also contains possibly incomplete results from the remote
        // side, we need to retrieve them from Discourse)

        if (installedAddonIds.contains(queryId)) {
            return cachedAddons.stream().filter(e -> queryId.equals(e.getUid())).findAny().orElse(null);
        }

        if (!remoteEnabled()) {
            return null;
        }

        // retrieve from remote
        try {
            URL url = URI.create(COMMUNITY_TOPIC_URL + uid.replace(ADDON_ID_PREFIX, "")).toURL();
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Accept", "application/json");
            if (this.apiKey != null) {
                connection.addRequestProperty("Api-Key", this.apiKey);
            }

            try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                DiscourseTopicResponseDTO parsed = gson.fromJson(reader, DiscourseTopicResponseDTO.class);
                return convertTopicToAddon(parsed);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        if (addonURI.toString().startsWith(COMMUNITY_TOPIC_URL)) {
            return addonURI.toString().substring(0, addonURI.toString().indexOf("/", COMMUNITY_BASE_URL.length()));
        }
        return null;
    }

    private @Nullable AddonType getAddonType(@Nullable Integer category, List<String> tags) {
        // check if we can determine the addon type from the category
        if (TRANSFORMATIONS_CATEGORY.equals(category)) {
            return AddonType.TRANSFORMATION;
        } else if (RULETEMPLATES_CATEGORY.equals(category)) {
            return AddonType.AUTOMATION;
        } else if (UIWIDGETS_CATEGORY.equals(category)) {
            return AddonType.UI;
        } else if (BLOCKLIBRARIES_CATEGORY.equals(category)) {
            return AddonType.AUTOMATION;
        } else if (BUNDLES_CATEGORY.equals(category)) {
            // try to get it from tags if we have tags
            return AddonType.DEFAULT_TYPES.stream().filter(type -> tags.contains(type.getId())).findFirst()
                    .orElse(null);
        }

        // or return null
        return null;
    }

    private Set<String> getValidResourceTypes(@Nullable String contentType) {
        if (contentType == null) {
            return Set.of();
        }
        Set<String> result = VALID_RESOURCE_TYPES.get(contentType);
        return result == null ? Set.of() : result;
    }

    private String getContentType(@Nullable Integer category, List<String> tags) {
        // check if we can determine the addon type from the category
        if (TRANSFORMATIONS_CATEGORY.equals(category)) {
            return TRANSFORMATIONS_CONTENT_TYPE;
        } else if (RULETEMPLATES_CATEGORY.equals(category)) {
            return RULETEMPLATES_CONTENT_TYPE;
        } else if (UIWIDGETS_CATEGORY.equals(category)) {
            return UIWIDGETS_CONTENT_TYPE;
        } else if (BLOCKLIBRARIES_CATEGORY.equals(category)) {
            return BLOCKLIBRARIES_CONTENT_TYPE;
        } else if (BUNDLES_CATEGORY.equals(category)) {
            if (tags.contains("kar")) {
                return KAR_CONTENT_TYPE;
            } else {
                // default to plain jar bundle for addons
                return JAR_CONTENT_TYPE;
            }
        }

        // empty string if content type could not be defined
        return "";
    }

    /**
     * Transforms a {@link DiscourseTopicItem} to an {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private @Nullable Addon convertTopicItemToAddon(DiscourseTopicItem topic, List<DiscourseUser> users) {
        try {
            List<String> tags = Arrays.asList(Objects.requireNonNullElse(topic.tags, new String[0]));

            String uid = ADDON_ID_PREFIX + topic.id.toString();
            AddonType addonType = getAddonType(topic.categoryId, tags);
            if (addonType == null) {
                logger.debug("Ignoring topic '{}' because no add-on type could be found", topic.id);
                return null;
            }
            String type = addonType.getId();
            String id = topic.id.toString(); // this will be replaced after installation by the correct id if available
            String contentType = getContentType(topic.categoryId, tags);

            String title = topic.title;
            boolean compatible = true;

            Matcher matcher = VersionRange.RANGE_PATTERN.matcher(title);
            if (matcher.find()) {
                try {
                    compatible = VersionRange.valueOf(matcher.group().trim()).includes(coreVersion);
                    title = title.substring(0, matcher.start());
                    logger.debug("{} is {}compatible with core version {}", topic.title, compatible ? "" : "NOT ", coreVersion);
                } catch (IllegalArgumentException e) {
                    logger.debug("Failed to determine compatibility for addon {}: {}", topic.title, e.getMessage());
                    compatible = true;
                }
            }

            String link = COMMUNITY_TOPIC_URL + topic.id.toString();
            int likeCount = topic.likeCount;
            int views = topic.views;
            int postsCount = topic.postsCount;
            Date createdDate = topic.createdAt;
            String author = "";
            for (DiscoursePosterInfo posterInfo : topic.posters) {
                if (posterInfo.description.contains("Original Poster")) {
                    author = users.stream().filter(u -> u.id.equals(posterInfo.userId)).findFirst().get().name;
                }
            }

            String maturity = tags.stream().filter(CODE_MATURITY_LEVELS::contains).findAny().orElse(null);

            Map<String, Object> properties = Map.of("created_at", createdDate, //
                    "like_count", likeCount, //
                    "views", views, //
                    "posts_count", postsCount, //
                    "tags", tags.toArray(String[]::new));

            // try to use a handler to determine if the add-on is installed
            boolean installed = addonHandlers.stream()
                    .anyMatch(handler -> handler.supports(type, contentType) && handler.isInstalled(uid));

            return Addon.create(uid).withType(type).withId(id).withContentType(contentType)
                    .withImageLink(topic.imageUrl).withAuthor(author).withProperties(properties).withLabel(title)
                    .withInstalled(installed).withMaturity(maturity).withCompatible(compatible).withLink(link).build();
        } catch (RuntimeException e) {
            logger.debug("Ignoring marketplace add-on '{}' due: {}", topic.title, e.getMessage());
            return null;
        }
    }

    /**
     * Unescapes occurrences of XML entities found in the supplied content.
     *
     * @param content the content with potentially escaped entities
     * @return the unescaped content
     */
    private String unescapeEntities(String content) {
        return content.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&amp;", "&");
    }

    /**
     * Transforms a {@link DiscourseTopicResponseDTO} to an {@link Addon}
     *
     * @param topic the topic
     * @return the list item
     */
    private Addon convertTopicToAddon(DiscourseTopicResponseDTO topic) {
        String uid = ADDON_ID_PREFIX + topic.id.toString();
        List<String> tags = Arrays.asList(Objects.requireNonNullElse(topic.tags, new String[0]));

        AddonType addonType = getAddonType(topic.categoryId, tags);
        String type = (addonType != null) ? addonType.getId() : "";
        String contentType = getContentType(topic.categoryId, tags);
        Set<String> validResourceTypes = getValidResourceTypes(contentType);
        List<MarketplaceAddonHandler> relevantHandlers = addonHandlers.stream().filter(handler -> handler.supports(type, contentType)).toList();

        int likeCount = topic.likeCount;
        int views = topic.views;
        int postsCount = topic.postsCount;
        Date createdDate = topic.postStream.posts[0].createdAt;
        Date updatedDate = topic.postStream.posts[0].updatedAt;
        Date lastPostedDate = topic.lastPosted;

        String maturity = tags.stream().filter(CODE_MATURITY_LEVELS::contains).findAny().orElse(null);

        Map<String, Object> properties = new HashMap<>(10);
        properties.put("created_at", createdDate);
        properties.put("updated_at", updatedDate);
        properties.put("last_posted", lastPostedDate);
        properties.put("like_count", likeCount);
        properties.put("views", views);
        properties.put("posts_count", postsCount);
        properties.put("tags", tags.toArray(String[]::new));

        String detailedDescription = topic.postStream.posts[0].cooked;

        VersionedAddon.Builder builder = new VersionedAddon.Builder(uid)
            .withType(type).withContentType(contentType)
            .withImageLink(topic.imageUrl).withLink(COMMUNITY_TOPIC_URL + topic.id.toString())
            .withAuthor(topic.postStream.posts[0].displayUsername).withMaturity(maturity);

        Matcher matcher = CODE_ADDON_PATTERN.matcher(detailedDescription);
        String s;
        if (matcher.find()) {
            String addonCode = unescapeEntities(matcher.group("content"));
            detailedDescription = matcher.replaceFirst("");
            matcher = KEY_VALUE_PATTERN.matcher(addonCode);
            while (matcher.find()) {
                switch (matcher.group("key").toLowerCase(Locale.ROOT)) {
                    case "version":
                        builder.withVersion(matcher.group("value"));
                        break;
                    case "keywords":
                        builder.withKeywords(matcher.group("value"));
                        break;
                    case "countries":
                        List<String> countries = Arrays.asList(matcher.group("value").trim().split("\\s*(?:,|;)\\s*"));
                        countries = countries.stream().filter(e -> !e.isBlank()).toList();
                        builder.withCountries(countries);
                        break;
                    case "license":
                        builder.withLicense(matcher.group("value"));
                        break;
                    case "connection":
                        s = matcher.group("value").toLowerCase(Locale.ROOT);
                        if ("local".equals(s) || "cloud".equals(s) || "hybrid".equals(s)
                            || "cloudDiscovery".equals(s)) {
                            builder.withConnection(s);
                        }
                        break;
                    case "loggerpackages":
                        List<String> loggerPackages = Arrays
                            .asList(matcher.group("value").trim().split("\\s*(?:,|;)\\s*"));
                        loggerPackages = loggerPackages.stream().filter(e -> !e.isBlank()).toList();
                        builder.withLoggerPackages(loggerPackages);
                        break;
                    case "documentation":
                        builder.withDocumentationLink(matcher.group("value"));
                        break;
                    case "issues":
                        builder.withIssuesLink(matcher.group("value"));
                        break;
                    default:
                        logger.debug("Ignoring unknown key:value pair \"{}:{}\" for Marketplace add-on \"{}\" addon entry", matcher.group("key"), matcher.group("value"), topic.title);
                        break;
                }
            }
        }

        AddonVersion.Builder versionBuilder;
        Matcher innerMatcher;
        while ((matcher = CODE_VERSION_PATTERN.matcher(detailedDescription)).find()) {
            versionBuilder = AddonVersion.create().withCompatible(true);
            String versionCode = unescapeEntities(matcher.group("content"));
            detailedDescription = matcher.replaceFirst("");
            Version version = null;
            boolean skip = false;
            boolean compatible = true;
            Map<String, Object> versionProperties = null;
            innerMatcher = KEY_VALUE_PATTERN.matcher(versionCode);
            while (!skip && innerMatcher.find()) {
                switch (innerMatcher.group("key").toLowerCase(Locale.ROOT)) {
                    case "version":
                        try {
                            s = innerMatcher.group("value");
                            if (!s.isBlank()) {
                                version = Version.valueOf(innerMatcher.group("value"));
                            }
                        } catch (IllegalArgumentException e) {
                            logger.debug("Invalid version \"{}\" specified for Marketplace add-on \"{}\" - skipping version entry", innerMatcher.group("value"), topic.title);
                            skip = true;
                        }
                        break;
                    case "corerange":
                        try {
                            VersionRange range = VersionRange.valueOf(innerMatcher.group("value"));
                            versionBuilder.withCoreRange(range);
                            compatible = range.includes(coreVersion);
                        } catch (IllegalArgumentException e) {
                            logger.debug("Invalid version range \"{}\" specified for Marketplace add-on \"{}\"", innerMatcher.group("value"), topic.title);
                        }
                        break;
                    case "maturity":
                        s = innerMatcher.group("value").trim().toLowerCase(Locale.ROOT);
                        if (CODE_MATURITY_LEVELS.contains(s)) {
                            versionBuilder.withMaturity(s);
                        }
                        break;
                    case "keywords":
                        versionBuilder.withKeywords(innerMatcher.group("value"));
                        break;
                    case "countries":
                        List<String> countries = Arrays.stream(innerMatcher.group("value").trim().split("\\s*(?:,|;)\\s*"))
                            .filter(e -> !e.isBlank()).toList();
                        versionBuilder.withCountries(countries);
                        break;
                    case "connection":
                        s = innerMatcher.group("value").toLowerCase(Locale.ROOT);
                        if ("local".equals(s) || "cloud".equals(s) || "hybrid".equals(s)
                            || "cloudDiscovery".equals(s)) {
                            builder.withConnection(s);
                        }
                        break;
                    case "loggerpackages":
                        List<String> loggerPackages = Arrays
                            .asList(innerMatcher.group("value").trim().split("\\s*(?:,|;)\\s*"));
                        loggerPackages = loggerPackages.stream().filter(e -> !e.isBlank()).toList();
                        versionBuilder.withLoggerPackages(loggerPackages);
                        break;
                    case "documentation":
                        versionBuilder.withDocumentationLink(innerMatcher.group("value"));
                        break;
                    case "issues":
                        versionBuilder.withIssuesLink(innerMatcher.group("value"));
                        break;
                    case "description":
                        versionBuilder.withDescription(innerMatcher.group("value"));
                        break;
                    case "url":
                        if (versionProperties == null) {
                            versionProperties = new HashMap<>();
                        }
                        s = innerMatcher.group("value").toLowerCase(Locale.ROOT);
                        int i = s.lastIndexOf('.');
                        if (i >= 0) {
                            String urlProperty;
                            switch (s.substring(i + 1)) {
                                case "jar":
                                    urlProperty = JAR_DOWNLOAD_URL_PROPERTY;
                                    break;
                                case "kar":
                                    urlProperty = KAR_DOWNLOAD_URL_PROPERTY;
                                    break;
                                case "json":
                                    urlProperty = JSON_DOWNLOAD_URL_PROPERTY;
                                    break;
                                case "yaml":
                                    urlProperty = YAML_DOWNLOAD_URL_PROPERTY;
                                    break;
                                default:
                                    urlProperty = null;
                                    break;
                            }
                            if (urlProperty != null) {
                                if (validResourceTypes.contains(urlProperty)) {
                                    versionProperties.put(urlProperty, s);
                                } else {
                                    logger.debug("Ignoring invalid version URL type \"{}\" for Marketplace add-on \"{}\"", urlProperty, topic.title);
                                }
                            } else {
                                logger.debug("Ignoring URL with unknown resource extension \"{}\" for Marketplace add-on \"{}\"", s.substring(i + 1), topic.title);
                            }
                        } else {
                            logger.debug("Unknown resource type for URL \"{}\" for Marketplace add-on \"{}\" - ignoring URL", s, topic.title);
                        }
                        break;
                    default:
                        logger.debug("Ignoring unknown key:value pair \"{}:{}\" for Marketplace add-on \"{}\" verion entry", innerMatcher.group("key"), innerMatcher.group("value"), topic.title);
                        break;
                }
            }
            if (skip) {
                continue;
            }
            if (version == null) {
                logger.debug("Skipping version entry without a version number for Marketplace add-on \"{}\" ", topic.title);
                continue;
            }

            if ((versionProperties == null || !versionProperties.keySet().stream().anyMatch(p -> validResourceTypes.contains(p))) && (validResourceTypes.contains(JSON_CONTENT_PROPERTY) || validResourceTypes.contains(YAML_CONTENT_PROPERTY))) {
                // Look for inline resource
                int pos = matcher.start();
                matcher = CODE_MARKUP_PATTERN.matcher(detailedDescription);
                if (matcher.find(pos)) {
                    if (("yaml".equals(s = matcher.group("lang").toLowerCase(Locale.ROOT)) && validResourceTypes.contains(YAML_CONTENT_PROPERTY)) || ("json".equals(s = matcher.group("lang").toLowerCase(Locale.ROOT)) && validResourceTypes.contains(JSON_CONTENT_PROPERTY))) {
                        if (versionProperties == null) {
                            versionProperties = new HashMap<>();
                        }
                        versionProperties.put(s + CODE_CONTENT_SUFFIX, unescapeEntities(matcher.group("content")));
                        detailedDescription = detailedDescription.substring(0, matcher.start()) + detailedDescription.substring(matcher.end());
                    }
                }
            }

            String versionUID = uid + ":v" + UIDUtils.encode(version.toString());
            versionBuilder.withProperties(versionProperties).withUID(versionUID).withVersion(version)
                    .withCompatible(compatible).withInstalled(relevantHandlers.stream().anyMatch(handler -> handler.isInstalled(versionUID)));
            if (versionBuilder.isValid(validResourceTypes)) { // TODO: (Nad)
                builder.withAddonVersion(versionBuilder.build());
            } else {
                //TODO: (Nad) Log
            }
        }

        String id = null;

        boolean resourceFound = false;
        boolean compatible = true;
        AddonVersion latestStable = null; //TODO: (Nad) Fallback to non-compatible
        SortedMap<Version, AddonVersion> versions = builder.getVersions();
        if (versions != null && !versions.isEmpty()) {
            compatible = false;
            List<Entry<String, Object>> props = versions.values().stream().filter(a -> !a.getProperties().isEmpty())
                    .flatMap(t -> t.getProperties().entrySet().stream()).filter(e -> RESOURCE_PROPERTY_NAMES.contains(e.getKey())).toList();
            for (Entry<String, Object> entry : props) {
                if (entry.getValue() instanceof String value && (JAR_DOWNLOAD_URL_PROPERTY.equals(value) || KAR_DOWNLOAD_URL_PROPERTY.equals(value))) {
                    id = determineIdFromUrl(value);
                    if (id != null) {
                        break;
                    }
                }
            }
            resourceFound = !props.isEmpty();
            //TODO: (Nad) Maybe redesign the above and do all checks in one big loop..?
            for (AddonVersion addonVersion : versions.values()) {
                compatible |= addonVersion.isCompatible();
                if (latestStable == null && addonVersion.isCompatible() && addonVersion.isStable()) {
                    latestStable = addonVersion;
                }
                if (compatible && latestStable != null) {
                    break;
                }
            }
        }

        // Gather resources in the "traditional way" if none are found using version sections
        if (!resourceFound && topic.postStream.posts[0].linkCounts != null) {
            for (DiscoursePostLink postLink : topic.postStream.posts[0].linkCounts) {
                if (postLink.url.endsWith(".jar") && validResourceTypes.contains(JAR_DOWNLOAD_URL_PROPERTY)) {
                    properties.put(JAR_DOWNLOAD_URL_PROPERTY, postLink.url);
                    id = determineIdFromUrl(postLink.url);
                    resourceFound = true;
                }
                if (postLink.url.endsWith(".kar") && validResourceTypes.contains(KAR_DOWNLOAD_URL_PROPERTY)) {
                    properties.put(KAR_DOWNLOAD_URL_PROPERTY, postLink.url);
                    id = determineIdFromUrl(postLink.url);
                    resourceFound = true;
                }
                if (postLink.url.endsWith(".json") && validResourceTypes.contains(JSON_DOWNLOAD_URL_PROPERTY)) {
                    properties.put(JSON_DOWNLOAD_URL_PROPERTY, postLink.url);
                    resourceFound = true;
                }
                if (postLink.url.endsWith(".yaml") && validResourceTypes.contains(YAML_DOWNLOAD_URL_PROPERTY)) {
                    properties.put(YAML_DOWNLOAD_URL_PROPERTY, postLink.url);
                    resourceFound = true;
                }
            }
        }
        if (!resourceFound) {
            matcher = CODE_INLINE_RESOURCE_PATTERN.matcher(detailedDescription);
            if (matcher.find()) {
                s = matcher.group("lang").toLowerCase(Locale.ROOT);
                if (("json".equals(s) && validResourceTypes.contains(JSON_CONTENT_PROPERTY)) || ("yaml".equals(s) && validResourceTypes.contains(YAML_CONTENT_PROPERTY))) {
                    properties.put(s + CODE_CONTENT_SUFFIX, unescapeEntities(matcher.group("content")));
                    detailedDescription = matcher.replaceFirst("");
                }
            }
        }

        if (id == null) {
            id = topic.id.toString(); // this is a fallback if we couldn't find a better id
        }

        // try to use a handler to determine if the add-on is installed
        builder.withInstalled(relevantHandlers.stream().anyMatch(handler -> handler.isInstalled(uid))); //TODO: (Nad) Apply logic?

        String title = topic.title;
        matcher = VersionRange.RANGE_PATTERN.matcher(title);
        if (matcher.find()) {
            if (versions == null || versions.isEmpty()) {
                compatible = VersionRange.valueOf(matcher.group().trim()).includes(coreVersion);
            }
            title = title.substring(0, matcher.start());
        }
        builder.withLabel(title).withId(id).withCompatible(compatible).withDetailedDescription(detailedDescription).withProperties(properties);
        //TODO: (Nad) Test
        if (latestStable != null) {
            builder.withCompatible(latestStable.isCompatible()).withInstalled(latestStable.isInstalled())
                    .withUid(latestStable.getUid());
            if (latestStable.getVersion() != null) {
                builder.withVersion(latestStable.getVersion().toString());
            }
            if (!latestStable.getCountries().isEmpty()) {
                if (builder.getCountries() == null) {
                    builder.withCountries(latestStable.getCountries());
                } else {
                    List<String> c = new ArrayList<>(builder.getCountries());
                    c.addAll(latestStable.getCountries());
                    builder.withCountries(c);
                }
            }
            //TODO: (Nad) Handle description
            if ((s = latestStable.getDocumentationLink()) != null) {
                builder.withDocumentationLink(s);
            }
            if ((s = latestStable.getIssuesLink()) != null) {
                builder.withIssuesLink(s);
            }
            if ((s = latestStable.getKeywords()) != null) { //TODO: (Nad) Combine?
                builder.withKeywords(s);
            }
            if (!latestStable.getLoggerPackages().isEmpty()) {
                if (builder.getLoggerPackages() == null) {
                    builder.withLoggerPackages(latestStable.getLoggerPackages());
                } else {
                    List<String> l = new ArrayList<>(builder.getLoggerPackages());
                    l.addAll(latestStable.getLoggerPackages());
                    builder.withLoggerPackages(l);
                }
            }
            if ((s = latestStable.getMaturity()) != null && !s.isBlank()) {
                builder.withMaturity(s);
            }

            properties.putAll(latestStable.getProperties());
            builder.withProperties(properties);
        }

        return builder.build();
    }

    private @Nullable String determineIdFromUrl(String url) {
        Matcher matcher = BUNDLE_NAME_PATTERN.matcher(url);
        if (matcher.matches()) {
            String bundleName = matcher.group(1);
            return bundleName.substring(bundleName.lastIndexOf(".") + 1);
        } else {
            logger.debug("Could not determine bundle name from url: {}", url);
        }
        return null;
    }
}
